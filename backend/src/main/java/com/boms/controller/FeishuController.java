package com.boms.controller;

import com.boms.model.SystemUser;
import com.boms.security.JwtTokenProvider;
import com.boms.service.FeishuService;
import com.boms.service.UserDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
@CrossOrigin(origins = "*")
public class FeishuController {

    @Autowired
    private FeishuService feishuService;

    @Autowired
    private UserDirectoryService userDirectoryService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${feishu.app-id}")
    private String appId;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("appId", appId);
        return config;
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> auth(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少授权码 (code)"));
        }

        Map<String, String> userInfo = feishuService.getUserInfoByCode(code);
        String platformUserId = userInfo.get("userId");
        if ("fallback_user".equals(platformUserId)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "登录失败：无法从飞书获取身份信息"));
        }

        SystemUser user = userDirectoryService.getOrCreateUser(platformUserId, userInfo.get("name"), userInfo.get("avatarUrl"), userInfo.get("employeeNo"));
        
        if (!user.isActive()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "登录失败：账号已被禁用"));
        }

        String token = jwtTokenProvider.generateToken(user.getPlatformUserId(), user.getName());
        
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getPlatformUserId());
        userMap.put("employeeNo", user.getEmployeeNo());
        userMap.put("platformUserId", user.getPlatformUserId());
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole());
        userMap.put("canViewAll", user.getCanViewAll());
        userMap.put("departmentId", user.getDepartmentId());
        userMap.put("avatarUrl", user.getAvatarUrl());
        userMap.put("departmentName", user.getDepartmentName());
        userMap.put("enabled", user.getEnabled());
        userMap.put("status", user.getStatus());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("token", token);
        result.put("user", userMap);
        return ResponseEntity.ok(result);
    }
}
