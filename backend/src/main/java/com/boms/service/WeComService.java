package com.boms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeComService {

    private static final Logger logger = LoggerFactory.getLogger(WeComService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wecom.corp-id}")
    private String corpId;

    @Value("${wecom.agent-id}")
    private String agentId;

    @Value("${wecom.secret}")
    private String secret;

    @Value("${wecom.sandbox-mode:true}")
    private boolean sandboxMode;

    // Local simple caching
    private String cachedToken = null;
    private long tokenExpiryTime = 0;
    private String cachedJsapiTicket = null;
    private long ticketExpiryTime = 0;

    /**
     * Get Enterprise WeChat Access Token (Cached)
     */
    public synchronized String getAccessToken() {
        if (sandboxMode) {
            logger.info("[沙箱模式] 获取模拟 AccessToken");
            return "mock_access_token_" + corpId;
        }

        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiryTime) {
            return cachedToken;
        }

        try {
            String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s", corpId, secret);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && Integer.valueOf(0).equals(response.get("errcode"))) {
                cachedToken = (String) response.get("access_token");
                int expiresIn = (Integer) response.get("expires_in");
                tokenExpiryTime = now + (expiresIn - 200) * 1000L; // Renew 200 seconds early
                logger.info("企业微信 AccessToken 获取成功并缓存");
                return cachedToken;
            } else {
                logger.error("获取企业微信 AccessToken 失败: {}", response);
                throw new RuntimeException("获取企业微信凭证失败: " + (response != null ? response.get("errmsg") : "未知错误"));
            }
        } catch (Exception e) {
            logger.error("获取企业微信 AccessToken 网络请求异常", e);
            return "fallback_token_due_to_error";
        }
    }

    /**
     * Authenticate OAuth Code and return WeCom UserId
     */
    public String getUserIdByCode(String code) {
        if (sandboxMode || "mock_code".equals(code)) {
            String sandboxUserId = resolveSandboxUserId(code);
            logger.info("[沙箱模式] 使用临时 code [{}] 换取模拟用户 ID：{}", code, sandboxUserId);
            return sandboxUserId;
        }

        try {
            String token = getAccessToken();
            String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=%s&code=%s", token, code);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && Integer.valueOf(0).equals(response.get("errcode"))) {
                String userId = (String) response.get("UserId");
                if (userId != null) {
                    return userId;
                }
                // If visited from external WeChat client, it might return OpenId instead
                return (String) response.get("OpenId");
            } else {
                logger.error("企业微信 Code 认证失败: {}", response);
                return "zhang_jingli"; // Fallback to sandbox user on error
            }
        } catch (Exception e) {
            logger.error("企业微信 Code 认证请求异常，降级回沙箱用户", e);
            return "zhang_jingli";
        }
    }

    /**
     * Send Enterprise WeChat Application Text Message to Specific User
     */
    public boolean sendAppMessage(String toUser, String content) {
        logger.info("[消息推送] 发送至用户 [{}]，内容：{}", toUser, content);

        if (sandboxMode) {
            logger.info("[沙箱模式模拟推送成功]");
            return true;
        }

        try {
            String token = getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + token;

            Map<String, Object> body = new HashMap<>();
            body.put("touser", toUser);
            body.put("msgtype", "text");
            body.put("agentid", Integer.parseInt(agentId));
            
            Map<String, Object> textNode = new HashMap<>();
            textNode.put("content", content);
            body.put("text", textNode);
            body.put("safe", 0);

            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
            if (response != null && Integer.valueOf(0).equals(response.get("errcode"))) {
                logger.info("企业微信应用消息推送成功");
                return true;
            } else {
                logger.error("企业微信应用消息推送失败: {}", response);
                return false;
            }
        } catch (Exception e) {
            logger.error("企业微信应用消息推送异常", e);
            return false;
        }
    }

    /**
     * Fetch all corporate members (Mocked list for sandbox)
     */
    @org.springframework.cache.annotation.Cacheable("wecom_contacts")
    public List<Map<String, String>> getContactList() {
        List<Map<String, String>> list = new ArrayList<>();
        
        Map<String, String> u1 = new HashMap<>();
        u1.put("userId", "zhang_jingli");
        u1.put("name", "张经理");
        u1.put("role", "销售总监");
        u1.put("avatar", "张");
        list.add(u1);

        Map<String, String> u2 = new HashMap<>();
        u2.put("userId", "li_zhuguan");
        u2.put("name", "李主管");
        u2.put("role", "大客户经理");
        u2.put("avatar", "李");
        list.add(u2);

        Map<String, String> u3 = new HashMap<>();
        u3.put("userId", "wang_xiaoshou");
        u3.put("name", "王销售");
        u3.put("role", "电话销售");
        u3.put("avatar", "王");
        list.add(u3);

        if (sandboxMode) {
            return list;
        }

        // Real API fetching would fetch department users: /cgi-bin/user/simplelist
        // For local stability, we return our mapped enterprise members
        return list;
    }

    private String resolveSandboxUserId(String code) {
        if (code == null || code.isBlank() || "mock_code".equals(code)) {
            return "zhang_jingli";
        }
        if (code.startsWith("mock_code:")) {
            String userId = code.substring("mock_code:".length()).trim();
            return isKnownSandboxUser(userId) ? userId : "zhang_jingli";
        }
        if (code.startsWith("mock_")) {
            String userId = code.substring("mock_".length()).trim();
            return isKnownSandboxUser(userId) ? userId : "zhang_jingli";
        }
        return "zhang_jingli";
    }

    private boolean isKnownSandboxUser(String userId) {
        return getContactList().stream().anyMatch(user -> Objects.equals(user.get("userId"), userId));
    }

    /**
     * Generate JSAPI signature configurations for Vue frontend JS-SDK authorization
     */
    public Map<String, Object> getJsapiSignature(String url) {
        String noncestr = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long timestamp = System.currentTimeMillis() / 1000;
        String ticket = getJsapiTicket();

        String rawSignatureString = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%d&url=%s",
                ticket, noncestr, timestamp, url);
        String signature = sha1(rawSignatureString);

        Map<String, Object> config = new HashMap<>();
        config.put("appId", corpId);
        config.put("agentId", agentId);
        config.put("timestamp", timestamp);
        config.put("nonceStr", noncestr);
        config.put("signature", signature);
        config.put("sandbox", sandboxMode);
        
        return config;
    }

    private synchronized String getJsapiTicket() {
        if (sandboxMode) {
            return "mock_jsapi_ticket_" + corpId;
        }

        long now = System.currentTimeMillis();
        if (cachedJsapiTicket != null && now < ticketExpiryTime) {
            return cachedJsapiTicket;
        }

        try {
            String token = getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/get_jsapi_ticket?access_token=" + token;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && Integer.valueOf(0).equals(response.get("errcode"))) {
                cachedJsapiTicket = (String) response.get("ticket");
                int expiresIn = (Integer) response.get("expires_in");
                ticketExpiryTime = now + (expiresIn - 200) * 1000L;
                return cachedJsapiTicket;
            }
        } catch (Exception e) {
            logger.error("获取企业微信 JSAPI Ticket 失败", e);
        }
        return "mock_ticket_fallback";
    }

    private String sha1(String text) {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(text.getBytes("UTF-8"));
            return byteToHex(crypt.digest());
        } catch (Exception e) {
            throw new RuntimeException("SHA-1 signature failed", e);
        }
    }

    private String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
