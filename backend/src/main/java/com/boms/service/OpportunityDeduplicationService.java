package com.boms.service;

import com.boms.model.Opportunity;
import com.boms.model.OpportunityEmbedding;
import com.boms.model.SystemConfig;
import com.boms.repository.OpportunityEmbeddingRepository;
import com.boms.repository.OpportunityRepository;
import com.boms.repository.SystemConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class OpportunityDeduplicationService {

    private static final Logger logger = LoggerFactory.getLogger(OpportunityDeduplicationService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OpportunityRepository oppRepository;

    @Autowired
    private OpportunityEmbeddingRepository embeddingRepository;

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private FeishuService feishuService;

    @Value("${feishu.sandbox-mode:false}")
    private boolean sandboxMode;

    /**
     * Get system configuration with dynamic database fallback
     */
    public String getConfig(String key, String defaultValue) {
        return configRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Calculate and return combined text representing the opportunity
     */
    public String getCombinedText(Opportunity opp) {
        String company = opp.getCompany() != null ? opp.getCompany() : "";
        String name = opp.getName() != null ? opp.getName() : "";
        String industry = opp.getIndustry() != null ? opp.getIndustry() : "";
        String deviceTypes = opp.getDeviceTypes() != null ? opp.getDeviceTypes() : "";
        String deviceModels = opp.getDeviceModels() != null ? opp.getDeviceModels() : "";
        
        String raw = company + " " + name + " " + industry + " " + deviceTypes + " " + deviceModels;
        return raw.trim().replaceAll("\\s+", " ");
    }

    /**
     * Calculate MD5 hash of text
     */
    private String getMd5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call Qwen text-embedding-v3 API to get the 1024-dimensional vector
     */
    private float[] fetchEmbeddingFromQwen(String text, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("Missing DashScope API key. Cannot fetch embedding.");
            return null;
        }

        String url = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        Map<String, Object> input = new HashMap<>();
        input.put("texts", Collections.singletonList(text));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("dimension", 1024);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "text-embedding-v3");
        requestBody.put("input", input);
        requestBody.put("parameters", parameters);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                Map output = (Map) body.get("output");
                if (output != null) {
                    List embeddings = (List) output.get("embeddings");
                    if (embeddings != null && !embeddings.isEmpty()) {
                        Map embObj = (Map) embeddings.get(0);
                        List<Number> vectorList = (List<Number>) embObj.get("embedding");
                        if (vectorList != null && vectorList.size() == 1024) {
                            float[] vector = new float[1024];
                            for (int i = 0; i < 1024; i++) {
                                vector[i] = vectorList.get(i).floatValue();
                            }
                            return vector;
                        }
                    }
                }
            }
            logger.error("Failed to fetch embedding from Qwen. Response: {}", response.getBody());
        } catch (Exception e) {
            logger.error("Error calling Qwen Embedding API", e);
        }
        return null;
    }

    /**
     * Get or calculate embedding vector for an opportunity
     */
    public float[] getOrCalculateEmbedding(Opportunity opp) {
        if (opp.getId() == null) {
            return null;
        }

        String combinedText = getCombinedText(opp);
        if (combinedText.isBlank()) {
            return null;
        }
        String md5 = getMd5(combinedText);

        // Check cache in database
        Optional<OpportunityEmbedding> existingOpt = embeddingRepository.findById(opp.getId());
        if (existingOpt.isPresent()) {
            OpportunityEmbedding cached = existingOpt.get();
            if (md5.equals(cached.getTextMd5())) {
                try {
                    return objectMapper.readValue(cached.getEmbeddingJson(), float[].class);
                } catch (Exception e) {
                    logger.error("Error parsing cached embedding JSON for opportunity {}", opp.getId(), e);
                }
            }
        }

        // Fetch from Qwen API
        String apiKey = getConfig("dashscope.api-key", System.getenv("DASHSCOPE_API_KEY"));
        float[] vector = fetchEmbeddingFromQwen(combinedText, apiKey);
        if (vector != null) {
            try {
                String json = objectMapper.writeValueAsString(vector);
                OpportunityEmbedding newEmb = new OpportunityEmbedding(opp.getId(), json, md5);
                embeddingRepository.save(newEmb);
                logger.info("Successfully fetched and cached embedding for opportunity ID: {}", opp.getId());
                return vector;
            } catch (JsonProcessingException e) {
                logger.error("Error serializing vector to JSON", e);
            }
        }
        return null;
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double calculateCosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Class to hold duplicate match details
     */
    public static class DuplicateMatch {
        private final Opportunity itemA;
        private final Opportunity itemB;
        private final double similarity;

        public DuplicateMatch(Opportunity itemA, Opportunity itemB, double similarity) {
            this.itemA = itemA;
            this.itemB = itemB;
            this.similarity = similarity;
        }

        public Opportunity getItemA() { return itemA; }
        public Opportunity getItemB() { return itemB; }
        public double getSimilarity() { return similarity; }
    }

    /**
     * Find semantic duplicates for a candidate opportunity in database
     */
    public List<DuplicateMatch> findDuplicates(Opportunity candidate, double threshold) {
        float[] candidateVec = getOrCalculateEmbedding(candidate);
        if (candidateVec == null) {
            // For unsaved candidates, we calculate the vector on the fly but don't save to db yet
            String combinedText = getCombinedText(candidate);
            if (combinedText.isBlank()) return Collections.emptyList();
            String apiKey = getConfig("dashscope.api-key", System.getenv("DASHSCOPE_API_KEY"));
            candidateVec = fetchEmbeddingFromQwen(combinedText, apiKey);
            if (candidateVec == null) return Collections.emptyList();
        }

        List<Opportunity> allOpps = oppRepository.findAll();
        List<DuplicateMatch> duplicates = new ArrayList<>();

        for (Opportunity opp : allOpps) {
            // Skip self
            if (candidate.getId() != null && candidate.getId().equals(opp.getId())) {
                continue;
            }

            float[] otherVec = getOrCalculateEmbedding(opp);
            if (otherVec == null) {
                continue;
            }

            double sim = calculateCosineSimilarity(candidateVec, otherVec);
            if (sim >= threshold) {
                duplicates.add(new DuplicateMatch(candidate, opp, sim));
            }
        }

        duplicates.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return duplicates;
    }

    /**
     * Perform full database scan for duplicate opportunities
     */
    public List<DuplicateMatch> performFullScan(double threshold) {
        logger.info("Starting full database semantic deduplication scan. Threshold: {}", threshold);
        List<Opportunity> allOpps = oppRepository.findAll();
        
        // 1. Pre-calculate / cache all embeddings
        List<float[]> vectors = new ArrayList<>(allOpps.size());
        for (Opportunity opp : allOpps) {
            vectors.add(getOrCalculateEmbedding(opp));
        }

        List<DuplicateMatch> duplicates = new ArrayList<>();
        
        // 2. Pairwise compare
        for (int i = 0; i < allOpps.size(); i++) {
            float[] vecA = vectors.get(i);
            if (vecA == null) continue;

            for (int j = i + 1; j < allOpps.size(); j++) {
                float[] vecB = vectors.get(j);
                if (vecB == null) continue;

                double sim = calculateCosineSimilarity(vecA, vecB);
                if (sim >= threshold) {
                    duplicates.add(new DuplicateMatch(allOpps.get(i), allOpps.get(j), sim));
                }
            }
        }

        duplicates.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        logger.info("Full scan complete. Found {} duplicate pair(s).", duplicates.size());
        return duplicates;
    }

    /**
     * Send Feishu alerts for duplicate opportunities
     */
    public void sendFeishuAlert(List<DuplicateMatch> duplicates) {
        if (duplicates == null || duplicates.isEmpty()) {
            return;
        }

        String adminOpenId = getConfig("dedup.admin-openid", System.getenv("ADMIN_FEISHU_OPENID"));
        if (adminOpenId == null || adminOpenId.isBlank()) {
            logger.warn("No 'dedup.admin-openid' configuration or 'ADMIN_FEISHU_OPENID' env variable found. Skipping Feishu alert.");
            return;
        }

        double threshold = Double.parseDouble(getConfig("dedup.threshold", "0.85"));

        // Format elements
        List<Map<String, Object>> elements = new ArrayList<>();
        
        Map<String, Object> headerText = new HashMap<>();
        headerText.put("tag", "lark_md");
        headerText.put("content", String.format("**在商机数据中检测到以下语义高度相似的重复记录：**\n(共检测出 %d 组重复。判重阈值: %.2f)", duplicates.size(), threshold));
        
        Map<String, Object> headerDiv = new HashMap<>();
        headerDiv.put("tag", "div");
        headerDiv.put("text", headerText);
        elements.add(headerDiv);

        // Display top 10 duplicates
        int displayLimit = Math.min(duplicates.size(), 10);
        for (int i = 0; i < displayLimit; i++) {
            DuplicateMatch match = duplicates.get(i);
            
            Map<String, Object> hr = new HashMap<>();
            hr.put("tag", "hr");
            elements.add(hr);

            Map<String, Object> dupText = new HashMap<>();
            dupText.put("tag", "lark_md");
            dupText.put("content", String.format(
                    "**[重复组 #%d] 相似度: %.1f%%**\n" +
                    "• **商机 A** (ID: %d):\n" +
                    "  - 名称: %s\n" +
                    "  - 公司: %s\n" +
                    "  - 设备型号: %s\n" +
                    "  - 提报人: %s (%s)\n" +
                    "• **商机 B** (ID: %d):\n" +
                    "  - 名称: %s\n" +
                    "  - 公司: %s\n" +
                    "  - 设备型号: %s\n" +
                    "  - 提报人: %s (%s)",
                    i + 1,
                    match.getSimilarity() * 100,
                    match.getItemA().getId(),
                    match.getItemA().getName(),
                    match.getItemA().getCompany(),
                    match.getItemA().getDeviceModels() != null ? match.getItemA().getDeviceModels() : "未填",
                    match.getItemA().getSubmitter() != null ? match.getItemA().getSubmitter() : "未知",
                    match.getItemA().getSubmitDate() != null ? match.getItemA().getSubmitDate() : "未知",
                    match.getItemB().getId(),
                    match.getItemB().getName(),
                    match.getItemB().getCompany(),
                    match.getItemB().getDeviceModels() != null ? match.getItemB().getDeviceModels() : "未填",
                    match.getItemB().getSubmitter() != null ? match.getItemB().getSubmitter() : "未知",
                    match.getItemB().getSubmitDate() != null ? match.getItemB().getSubmitDate() : "未知"
            ));

            Map<String, Object> dupDiv = new HashMap<>();
            dupDiv.put("tag", "div");
            dupDiv.put("text", dupText);
            elements.add(dupDiv);
        }

        Map<String, Object> footerHr = new HashMap<>();
        footerHr.put("tag", "hr");
        elements.add(footerHr);

        Map<String, Object> footerPlain = new HashMap<>();
        footerPlain.put("tag", "plain_text");
        footerPlain.put("content", "提示：以上相似数据可能属于同一客户的同一需求，请管理员核实是否需要合并或驳回。");

        List<Map<String, Object>> footerElements = new ArrayList<>();
        footerElements.add(footerPlain);

        Map<String, Object> note = new HashMap<>();
        note.put("tag", "note");
        note.put("elements", footerElements);
        elements.add(note);

        // Card header
        Map<String, Object> titleObj = new HashMap<>();
        titleObj.put("tag", "plain_text");
        titleObj.put("content", "⚠️ 商机查重预警通知");

        Map<String, Object> cardHeader = new HashMap<>();
        cardHeader.put("title", titleObj);
        cardHeader.put("template", "red");

        // Card config
        Map<String, Object> config = new HashMap<>();
        config.put("wide_screen_mode", true);

        // Assemble card
        Map<String, Object> card = new HashMap<>();
        card.put("config", config);
        card.put("header", cardHeader);
        card.put("elements", elements);

        logger.info("Sending Feishu message card alert to administrator OpenID: {}", adminOpenId);
        boolean success = feishuService.sendInteractiveCard(adminOpenId, card);
        if (success) {
            logger.info("Successfully sent Feishu alert.");
        } else {
            logger.error("Failed to send Feishu alert card.");
        }
    }
}
