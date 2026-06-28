package com.boms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FeishuService {

    private static final Logger logger = LoggerFactory.getLogger(FeishuService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${feishu.app-id}")
    private String appId;

    @Value("${feishu.app-secret}")
    private String appSecret;

    @Value("${feishu.sandbox-mode:false}")
    private boolean sandboxMode;

    private String cachedToken = null;
    private long tokenExpiryTime = 0;

    public synchronized String getTenantAccessToken() {
        if (sandboxMode) return "mock_token";
        
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiryTime) {
            return cachedToken;
        }

        String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
        Map<String, String> body = new HashMap<>();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            if (responseBody.containsKey("tenant_access_token")) {
                cachedToken = (String) responseBody.get("tenant_access_token");
                Integer expire = (Integer) responseBody.get("expire");
                tokenExpiryTime = now + (expire - 200) * 1000L;
                return cachedToken;
            } else {
                logger.error("Feishu Token Error: {}", responseBody);
                throw new RuntimeException("Failed to get token: " + responseBody.get("msg"));
            }
        }
        throw new RuntimeException("HTTP Error getting Feishu token");
    }

    public Map<String, String> getUserInfoByCode(String code) {
        if (sandboxMode) return Map.of("userId", "zhang_jingli", "name", "张经理", "avatarUrl", "", "employeeNo", "zhang_jingli");
        if (code != null && code.startsWith("mock_code:")) {
            String val = code.substring(10);
            return Map.of("userId", val, "name", val, "avatarUrl", "", "employeeNo", val);
        }
        if ("mock_code".equals(code)) return Map.of("userId", "zhang_jingli", "name", "张经理", "avatarUrl", "", "employeeNo", "zhang_jingli");
        
        String token = getTenantAccessToken();
        String url = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("access_token")) {
                    String userAccessToken = (String) data.get("access_token");
                    
                    String userInfoUrl = "https://open.feishu.cn/open-apis/authen/v1/user_info";
                    HttpHeaders userInfoHeaders = new HttpHeaders();
                    userInfoHeaders.setBearerAuth(userAccessToken);
                    HttpEntity<?> userInfoRequest = new HttpEntity<>(userInfoHeaders);
                    ResponseEntity<Map> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userInfoRequest, Map.class);
                    
                    if (userInfoResponse.getStatusCode().is2xxSuccessful() && userInfoResponse.getBody() != null) {
                        Map<String, Object> userInfoData = (Map<String, Object>) userInfoResponse.getBody().get("data");
                        if (userInfoData != null) {
                                String openId = (String) userInfoData.get("open_id");
                                String feishuUserId = (String) userInfoData.get("user_id");
                                String userId = openId != null ? openId : feishuUserId;
                                if (userId != null) {
                                    String name = (String) userInfoData.get("name");
                                    String avatarUrl = (String) userInfoData.get("avatar_url");
                                    Map<String, String> result = new HashMap<>();
                                    result.put("userId", userId);
                                    result.put("employeeNo", feishuUserId);
                                    result.put("name", name != null ? name : userId);
                                    result.put("avatarUrl", avatarUrl != null ? avatarUrl : "");
                                    return result;
                            }
                        }
                    }
                }
            }
            logger.error("Feishu OAuth Error: {}", response.getBody());
        } catch(Exception e) {
            logger.error("Feishu OAuth Exception", e);
        }
        return Map.of("userId", "fallback_user", "name", "Fallback User", "avatarUrl", "");
    }

    public List<Map<String, Object>> getDepartments() {
        if (sandboxMode) return Collections.emptyList();
        
        try {
            String token = getTenantAccessToken();
            String url = "https://open.feishu.cn/open-apis/contact/v3/departments/0/children?fetch_child=true&department_id_type=department_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("items")) {
                    return (List<Map<String, Object>>) data.get("items");
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException.Forbidden e) {
            logger.warn("No authority on root department 0. Falling back to fetching scopes...");
            return getDepartmentsFromScopes();
        } catch (Exception e) {
            logger.error("Error getting departments from root", e);
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> getDepartmentsFromScopes() {
        List<String> deptIds = getAuthorizedDepartmentIds();
        if (deptIds == null || deptIds.isEmpty()) {
            logger.warn("No authorized department IDs found in scopes.");
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> allDepts = new ArrayList<>();
        Set<String> processedDepts = new HashSet<>();
        
        for (String deptId : deptIds) {
            if (processedDepts.contains(deptId)) continue;
            
            // Get current department info
            Map<String, Object> deptInfo = getDepartment(deptId);
            if (deptInfo != null) {
                allDepts.add(deptInfo);
                processedDepts.add(deptId);
            }
            
            // Get sub departments recursively
            List<Map<String, Object>> subDepts = getSubDepartments(deptId);
            for (Map<String, Object> subDept : subDepts) {
                String subId = (String) subDept.get("department_id");
                if (subId != null && !processedDepts.contains(subId)) {
                    allDepts.add(subDept);
                    processedDepts.add(subId);
                }
            }
        }
        return allDepts;
    }

    public List<String> getAuthorizedDepartmentIds() {
        if (sandboxMode) return Collections.emptyList();
        try {
            String token = getTenantAccessToken();
            String url = "https://open.feishu.cn/open-apis/contact/v3/scopes?department_id_type=department_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("department_ids")) {
                    return (List<String>) data.get("department_ids");
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching authorized scopes from Feishu", e);
        }
        return Collections.emptyList();
    }

    public Map<String, Object> getDepartment(String departmentId) {
        if (sandboxMode) return null;
        try {
            String token = getTenantAccessToken();
            String url = "https://open.feishu.cn/open-apis/contact/v3/departments/" + departmentId + "?department_id_type=department_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("department")) {
                    return (Map<String, Object>) data.get("department");
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching department " + departmentId + " from Feishu", e);
        }
        return null;
    }

    public List<Map<String, Object>> getSubDepartments(String departmentId) {
        if (sandboxMode) return Collections.emptyList();
        try {
            String token = getTenantAccessToken();
            String url = "https://open.feishu.cn/open-apis/contact/v3/departments/" + departmentId + "/children?fetch_child=true&department_id_type=department_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                if (data != null && data.containsKey("items")) {
                    return (List<Map<String, Object>>) data.get("items");
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching children for department " + departmentId + " from Feishu", e);
        }
        return Collections.emptyList();
    }

    public List<Map<String, Object>> getUsers(String departmentId) {
        if (sandboxMode) return Collections.emptyList();
        
        String token = getTenantAccessToken();
        String url = "https://open.feishu.cn/open-apis/contact/v3/users/find_by_department?department_id=" + departmentId + "&department_id_type=department_id";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            if (data != null) {
                if (data.containsKey("user_infos")) {
                    return (List<Map<String, Object>>) data.get("user_infos");
                } else if (data.containsKey("items")) {
                    return (List<Map<String, Object>>) data.get("items");
                }
            }
        }
        return Collections.emptyList();
    }

    public boolean sendAppMessage(String toUserId, String messageContent) {
        if (sandboxMode) return true;
        try {
            String token = getTenantAccessToken();
            String receiveIdType = toUserId.startsWith("ou_") ? "open_id" : "user_id";
            String url = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=" + receiveIdType;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            
            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", toUserId);
            body.put("msg_type", "text");
            String escapedMessage = messageContent.replace("\"", "\\\"").replace("\n", "\\n");
            body.put("content", "{\"text\":\"" + escapedMessage + "\"}");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    return true;
                } else {
                    logger.error("Feishu Message Failed, response: {}", response.getBody());
                }
            } else {
                logger.error("Feishu Message HTTP Error: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Feishu Message Error", e);
        }
        return false;
    }

    public boolean sendInteractiveCard(String toUserId, Map<String, Object> cardMap) {
        if (sandboxMode) return true;
        try {
            String token = getTenantAccessToken();
            String url = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            
            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", toUserId);
            body.put("msg_type", "interactive");
            body.put("content", new ObjectMapper().writeValueAsString(cardMap));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                return code != null && code == 0;
            }
        } catch (Exception e) {
            logger.error("Feishu Interactive Card Error", e);
        }
        return false;
    }
}
