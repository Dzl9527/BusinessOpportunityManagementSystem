package com.boms.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FeishuService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String getTenantAccessToken(String appId, String appSecret) {
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
                return (String) responseBody.get("tenant_access_token");
            } else {
                throw new RuntimeException("Failed to get token: " + responseBody.get("msg"));
            }
        }
        throw new RuntimeException("HTTP Error getting Feishu token");
    }

    public boolean sendTestMessage(String appId, String appSecret, String targetOpenId, String messageContent) {
        try {
            String token = getTenantAccessToken(appId, appSecret);
            String url = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            
            Map<String, Object> body = new HashMap<>();
            body.put("receive_id", targetOpenId);
            body.put("msg_type", "text");
            // Properly escape the JSON text inside content
            String escapedMessage = messageContent.replace("\"", "\\\"").replace("\n", "\\n");
            body.put("content", "{\"text\":\"" + escapedMessage + "\"}");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Integer code = (Integer) response.getBody().get("code");
                if (code != null && code == 0) {
                    return true;
                } else {
                    System.err.println("Feishu API Error: " + response.getBody().get("msg"));
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
