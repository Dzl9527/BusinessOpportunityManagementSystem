package com.boms.controller;

import com.boms.model.SystemUser;
import com.boms.model.UserVisibilityRule;
import com.boms.repository.UserVisibilityRuleRepository;
import com.boms.service.UserDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserDirectoryService userDirectoryService;

    @Autowired
    private UserVisibilityRuleRepository visibilityRuleRepository;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String getAdminUserId(String adminUserId) {
        String securityId = com.boms.security.SecurityUtils.getCurrentUserId();
        if (securityId != null) return securityId;
        return (adminUserId != null && !adminUserId.trim().isEmpty()) ? adminUserId.trim() : "anonymous";
    }

    private String getAdminName(String adminName) {
        return adminName == null || adminName.trim().isEmpty() ? "未登录用户" : adminName.trim();
    }

    private boolean isAdmin(String adminUserId, String adminName) {
        String resolvedId = getAdminUserId(adminUserId);
        String resolvedName = getAdminName(adminName);
        System.out.println("[UserController.isAdmin] resolvedId=" + resolvedId + ", resolvedName=" + resolvedName);
        SystemUser user = userDirectoryService.getCurrentUser(resolvedId, resolvedName);
        System.out.println("[UserController.isAdmin] user=" + (user != null ? user.getName() + " role=" + user.getRole() + " active=" + user.isActive() : "null"));
        return user != null && user.isActive() && user.isAdmin();
    }

    @GetMapping
    public ResponseEntity<List<SystemUser>> listUsers() {
        return ResponseEntity.ok(userDirectoryService.listUsers());
    }

    @GetMapping("/me")
    public SystemUser me(@RequestParam(required = false) String userId,
                         @RequestParam(required = false) String userName) {
        String securityId = com.boms.security.SecurityUtils.getCurrentUserId();
        if (securityId == null) { securityId = userId; }
        return userDirectoryService.getCurrentUser(securityId, userName);
    }

    @PostMapping("/sync-platform")
    public ResponseEntity<List<SystemUser>> syncPlatform(@RequestParam(required = false) String adminUserId,
                                                      @RequestParam(required = false) String adminName) {
        if (!isAdmin(adminUserId, adminName)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(userDirectoryService.syncFromPlatform());
    }

    @PutMapping("/{platformUserId}/role")
    public ResponseEntity<SystemUser> updateRole(@PathVariable String platformUserId,
                                                 @RequestBody Map<String, Object> body,
                                                 @RequestParam(required = false) String adminUserId,
                                                 @RequestParam(required = false) String adminName) {
        if (!isAdmin(adminUserId, adminName)) {
            return ResponseEntity.status(403).build();
        }
        String role = String.valueOf(body.getOrDefault("role", "USER"));
        Boolean canViewAll = Boolean.TRUE.equals(body.get("canViewAll"));
        return ResponseEntity.ok(userDirectoryService.updateRole(platformUserId, role, canViewAll));
    }

    @PutMapping("/{platformUserId}/enabled")
    public ResponseEntity<SystemUser> updateEnabled(@PathVariable String platformUserId,
                                                    @RequestBody Map<String, Object> body,
                                                    @RequestParam(required = false) String adminUserId,
                                                    @RequestParam(required = false) String adminName) {
        if (!isAdmin(adminUserId, adminName)) {
            return ResponseEntity.status(403).build();
        }
        Boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        return ResponseEntity.ok(userDirectoryService.updateEnabled(platformUserId, enabled));
    }

    @GetMapping("/{platformUserId}/visibility-rules")
    public ResponseEntity<List<UserVisibilityRule>> getVisibilityRules(@PathVariable String platformUserId,
                                                                       @RequestParam(required = false) String adminUserId,
                                                                       @RequestParam(required = false) String adminName) {
        if (!isAdmin(adminUserId, adminName)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(visibilityRuleRepository.findByViewerUserIdAndCanViewTrue(platformUserId));
    }

    @PutMapping("/{platformUserId}/visibility-rules")
    @Transactional
    public ResponseEntity<List<UserVisibilityRule>> saveVisibilityRules(@PathVariable String platformUserId,
                                                                        @RequestBody Map<String, Object> body,
                                                                        @RequestParam(required = false) String adminUserId,
                                                                        @RequestParam(required = false) String adminName) {
        if (!isAdmin(adminUserId, adminName)) {
            return ResponseEntity.status(403).build();
        }
        visibilityRuleRepository.deleteByViewerUserId(platformUserId);
        Object raw = body.get("visibleUserIds");
        List<String> visibleUserIds = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    visibleUserIds.add(String.valueOf(item));
                }
            }
        }
        List<UserVisibilityRule> saved = new ArrayList<>();
        for (String visibleUserId : visibleUserIds) {
            UserVisibilityRule rule = new UserVisibilityRule(platformUserId, visibleUserId, true, true);
            rule.setCreatedByUserId(adminUserId);
            rule.setCreatedByName(adminName);
            rule.setCreatedAt(now());
            rule.setUpdatedAt(now());
            saved.add(visibilityRuleRepository.save(rule));
        }
        return ResponseEntity.ok(saved);
    }
}
