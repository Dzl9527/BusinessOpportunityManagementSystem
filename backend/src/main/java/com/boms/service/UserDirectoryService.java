package com.boms.service;

import com.boms.model.SystemUser;
import com.boms.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class UserDirectoryService {
    @Autowired
    private SystemUserRepository userRepository;

    @Autowired
    private WeComService weComService;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public SystemUser getOrCreateUser(String wecomUserId, String name) {
        String resolvedUserId = wecomUserId == null || wecomUserId.isBlank() ? "zhang_jingli" : wecomUserId.trim();
        String resolvedName = name == null || name.isBlank() ? "张经理" : name.trim();
        SystemUser user = userRepository.findByWecomUserId(resolvedUserId).orElseGet(() -> {
            SystemUser created = new SystemUser();
            created.setWecomUserId(resolvedUserId);
            created.setName(resolvedName);
            created.setRole("USER");
            created.setCanViewAll(false);
            created.setEnabled(true);
            created.setStatus("ACTIVE");
            created.setCreatedAt(now());
            created.setUpdatedAt(now());
            return userRepository.save(created);
        });
        if ("zhang_jingli".equals(resolvedUserId) && !user.isAdmin()) {
            user.setRole("ADMIN");
            user.setCanViewAll(true);
            user.setAdminSource(user.getAdminSource() == null ? "SANDBOX" : user.getAdminSource());
            user.setUpdatedAt(now());
            return userRepository.save(user);
        }
        return user;
    }

    public SystemUser getCurrentUser(String userId, String userName) {
        return getOrCreateUser(userId, userName);
    }

    public List<SystemUser> listUsers() {
        ensureSandboxUsers();
        return userRepository.findAll();
    }

    public List<SystemUser> syncFromWeCom() {
        List<Map<String, String>> contacts = weComService.getContactList();
        for (Map<String, String> contact : contacts) {
            String userId = contact.get("userId");
            String name = contact.getOrDefault("name", userId);
            SystemUser user = getOrCreateUser(userId, name);
            user.setName(name);
            user.setPosition(contact.get("role"));
            user.setLastSyncedAt(now());
            user.setUpdatedAt(now());
            if ("zhang_jingli".equals(userId) && (user.getRole() == null || "USER".equals(user.getRole()))) {
                user.setRole("ADMIN");
                user.setCanViewAll(true);
                user.setAdminSource("SANDBOX");
            }
            userRepository.save(user);
        }
        return userRepository.findAll();
    }

    public void ensureSandboxUsers() {
        if (userRepository.count() == 0) {
            syncFromWeCom();
        }
    }

    public SystemUser updateRole(String wecomUserId, String role, Boolean canViewAll) {
        SystemUser user = getOrCreateUser(wecomUserId, wecomUserId);
        user.setRole(role == null || role.isBlank() ? "USER" : role);
        user.setCanViewAll(Boolean.TRUE.equals(canViewAll) || "ADMIN".equalsIgnoreCase(role));
        user.setAdminSource("MANUAL");
        user.setUpdatedAt(now());
        return userRepository.save(user);
    }

    public SystemUser updateEnabled(String wecomUserId, Boolean enabled) {
        SystemUser user = getOrCreateUser(wecomUserId, wecomUserId);
        user.setEnabled(Boolean.TRUE.equals(enabled));
        user.setStatus(Boolean.TRUE.equals(enabled) ? "ACTIVE" : "DISABLED");
        user.setUpdatedAt(now());
        return userRepository.save(user);
    }
}
