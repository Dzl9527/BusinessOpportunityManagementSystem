package com.boms.controller;

import com.boms.model.Opportunity;
import com.boms.model.SystemUser;
import com.boms.repository.OpportunityRepository;
import com.boms.service.UserDirectoryService;
import com.boms.service.WeComService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wecom")
@CrossOrigin(origins = "*")
public class WeComController {

    @Autowired
    private WeComService weComService;

    @Autowired
    private OpportunityRepository oppRepository;

    @Autowired
    private UserDirectoryService userDirectoryService;

    private String getCurrentUserId(String userId) {
        return userId == null || userId.trim().isEmpty() ? "anonymous" : userId.trim();
    }

    private String getCurrentUserName(String userName) {
        return userName == null || userName.trim().isEmpty() ? "未登录用户" : userName.trim();
    }

    private boolean isAdmin(String userId, String userName) {
        SystemUser user = userDirectoryService.getCurrentUser(getCurrentUserId(userId), getCurrentUserName(userName));
        return user != null && user.isActive() && user.isAdmin();
    }

    /**
     * Frontend logins callback code auth
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestParam String code) {
        String userId = weComService.getUserIdByCode(code);
        
        // Find matching contact profile details
        List<Map<String, String>> contacts = weComService.getContactList();
        Map<String, String> userProfile = contacts.stream()
                .filter(u -> u.get("userId").equals(userId))
                .findFirst()
                .orElse(contacts.get(0)); // Default to Zhang manager

        SystemUser localUser = userDirectoryService.getOrCreateUser(userId, userProfile.get("name"));
        if (!localUser.isActive()) {
            Map<String, Object> blocked = new HashMap<>();
            blocked.put("message", "当前企业微信用户已禁用或离职，无法登录");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(blocked);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", "jwt_token_wecom_session_" + System.currentTimeMillis());
        result.put("userId", userId);
        result.put("name", localUser.getName());
        result.put("role", localUser.getRole());
        result.put("position", userProfile.get("role"));
        result.put("canViewAll", localUser.getCanViewAll());
        result.put("enabled", localUser.getEnabled());
        result.put("avatar", userProfile.get("avatar"));

        return ResponseEntity.ok(result);
    }

    /**
     * Get corporate contacts for team view / assign selection
     */
    @GetMapping("/contacts")
    public ResponseEntity<List<Map<String, String>>> getContacts() {
        return ResponseEntity.ok(weComService.getContactList());
    }

    /**
     * Get JSAPI signature for frontend WeChat client config
     */
    @GetMapping("/jsapi-signature")
    public ResponseEntity<Map<String, Object>> getSignature(@RequestParam String url) {
        return ResponseEntity.ok(weComService.getJsapiSignature(url));
    }

    /**
     * Send test message from settings page
     */
    @PostMapping("/push-test")
    public ResponseEntity<Map<String, Object>> testPush(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "来自商机系统的企业微信测试消息。");
        boolean success = weComService.sendAppMessage("zhang_jingli", content);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "推送测试发送成功，请前往企微客户端查看！" : "推送失败，请检查密钥配置。");
        return ResponseEntity.ok(response);
    }

    /**
     * Export all opportunities as JSON file
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOpportunities(@RequestParam(required = false) String userId,
                                                      @RequestParam(required = false) String userName) {
        if (!isAdmin(userId, userName)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Opportunity> all = oppRepository.findAll();
        // Since we return entities with bidirectional JsonManagedReference/JsonBackReference, 
        // we can serialize them directly into standard JSON format
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(all);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "BOMS_Opportunities_Backup.json");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import opportunities list from backup file
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importOpportunities(@RequestBody List<Opportunity> importedOpps,
                                                                   @RequestParam(required = false) String userId,
                                                                   @RequestParam(required = false) String userName) {
        Map<String, Object> response = new HashMap<>();
        if (!isAdmin(userId, userName)) {
            response.put("success", false);
            response.put("message", "仅管理员可导入或覆盖商机数据");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        try {
            if (importedOpps == null || importedOpps.isEmpty()) {
                response.put("success", false);
                response.put("message", "导入的商机列表为空");
                return ResponseEntity.badRequest().body(response);
            }

            // Clear old values and save imported. To prevent FK constraints, clear properly.
            oppRepository.deleteAll();
            
            // Re-establish bidirectional links
            for (Opportunity opp : importedOpps) {
                opp.setId(null); // Force database recreation
                if (opp.getTasks() != null) {
                    opp.getTasks().forEach(t -> {
                        t.setId(null);
                        t.setOpportunity(opp);
                    });
                }
                if (opp.getActivities() != null) {
                    opp.getActivities().forEach(a -> {
                        a.setId(null);
                        a.setOpportunity(opp);
                    });
                }
            }
            oppRepository.saveAll(importedOpps);

            response.put("success", true);
            response.put("message", "成功恢复 " + importedOpps.size() + " 个商机！");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "解析或导入数据失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
