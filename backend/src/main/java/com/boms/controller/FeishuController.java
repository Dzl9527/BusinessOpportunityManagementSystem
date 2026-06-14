package com.boms.controller;

import com.boms.service.FeishuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
public class FeishuController {

    @Autowired
    private FeishuService feishuService;

    @PostMapping("/push-test")
    public ResponseEntity<Map<String, Object>> testPush(@RequestBody Map<String, String> payload) {
        String appId = payload.get("appId");
        String appSecret = payload.get("appSecret");
        String targetOpenId = payload.get("targetOpenId");
        String message = payload.get("message");

        Map<String, Object> result = new HashMap<>();
        
        if (appId == null || appId.trim().isEmpty() || appSecret == null || appSecret.trim().isEmpty() || 
            targetOpenId == null || targetOpenId.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "缺少必填参数 (App ID, Secret, Open ID, 测试内容)，请补全后再试");
            return ResponseEntity.badRequest().body(result);
        }

        boolean success = feishuService.sendTestMessage(appId, appSecret, targetOpenId, message);
        
        result.put("success", success);
        result.put("message", success ? "飞书测试消息发送成功！" : "飞书消息发送失败，请检查您的凭证或 Open ID 是否有效");
        
        return ResponseEntity.ok(result);
    }
}
