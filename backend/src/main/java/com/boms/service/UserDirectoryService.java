package com.boms.service;

import com.boms.model.SystemDepartment;
import com.boms.model.SystemUser;
import com.boms.repository.SystemDepartmentRepository;
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
    private SystemDepartmentRepository departmentRepository;

    @Autowired
    private FeishuService feishuService;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public SystemUser getOrCreateUser(String platformUserId, String name, String avatarUrl) {
        String resolvedUserId = platformUserId == null || platformUserId.isBlank() ? "zhang_jingli" : platformUserId.trim();
        String resolvedName = name == null || name.isBlank() ? "张经理" : name.trim();
        SystemUser user = userRepository.findByPlatformUserIdOrWecomUserId(resolvedUserId, resolvedUserId).orElseGet(() -> {
            SystemUser created = new SystemUser();
            created.setPlatformUserId(resolvedUserId);
            created.setWecomUserId(resolvedUserId);
            created.setName(resolvedName);
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                created.setAvatarUrl(avatarUrl);
            }
            created.setRole("USER");
            created.setCanViewAll(false);
            created.setEnabled(true);
            created.setStatus("ACTIVE");
            created.setCreatedAt(now());
            created.setUpdatedAt(now());
            return userRepository.save(created);
        });
        
        boolean needsUpdate = false;
        if (user.getPlatformUserId() == null || user.getPlatformUserId().isBlank()) {
            user.setPlatformUserId(resolvedUserId);
            needsUpdate = true;
        }
        if (user.getWecomUserId() == null || user.getWecomUserId().isBlank()) {
            user.setWecomUserId(resolvedUserId);
            needsUpdate = true;
        }
        if (avatarUrl != null && !avatarUrl.isBlank() && !avatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl);
            needsUpdate = true;
        }
        if (!resolvedName.equals(user.getName())) {
            user.setName(resolvedName);
            needsUpdate = true;
        }
        if (needsUpdate) {
            user.setUpdatedAt(now());
            user = userRepository.save(user);
        }
        if (("zhang_jingli".equals(resolvedUserId) || "邓钟璐".equals(resolvedName)) && !user.isAdmin()) {
            user.setRole("ADMIN");
            user.setCanViewAll(true);
            user.setAdminSource(user.getAdminSource() == null ? "SYSTEM_INIT" : user.getAdminSource());
            user.setUpdatedAt(now());
            return userRepository.save(user);
        }
        return user;
    }

    public SystemUser getCurrentUser(String userId, String userName) {
        return getOrCreateUser(userId, userName, null);
    }

    public List<SystemUser> listUsers() {
        ensureSandboxUsers();
        return userRepository.findAll();
    }

    public List<SystemUser> syncFromPlatform() {
        // 1. Sync Departments
        System.out.println("[syncFromPlatform] Starting sync...");
        List<Map<String, Object>> depts = feishuService.getDepartments();
        System.out.println("[syncFromPlatform] Got " + depts.size() + " departments from Feishu");
        for (Map<String, Object> deptInfo : depts) {
            String deptId = (String) deptInfo.get("department_id");
            System.out.println("[syncFromPlatform] Processing dept: " + deptId + " name=" + deptInfo.get("name"));
            SystemDepartment dept = departmentRepository.findByDepartmentId(deptId).orElse(new SystemDepartment());
            dept.setDepartmentId(deptId);
            dept.setName((String) deptInfo.get("name"));
            dept.setParentId((String) deptInfo.get("parent_department_id"));
            dept.setLeaderUserId((String) deptInfo.get("leader_user_id"));
            departmentRepository.save(dept);
            
            // 2. Sync Users for each department
            List<Map<String, Object>> users = feishuService.getUsers(deptId);
            System.out.println("[syncFromPlatform] Dept " + deptId + " has " + users.size() + " users");
            for (Map<String, Object> userInfo : users) {
                String openId = (String) userInfo.get("open_id");
                if (openId == null) openId = (String) userInfo.get("user_id");
                if (openId == null) continue;
                
                String name = (String) userInfo.get("name");
                String avatarUrl = null;
                if (userInfo.containsKey("avatar_url")) {
                    avatarUrl = (String) userInfo.get("avatar_url");
                }
                if (avatarUrl == null && userInfo.containsKey("avatar")) {
                    Object avatarObj = userInfo.get("avatar");
                    if (avatarObj instanceof Map) {
                        Map<String, String> avatarMap = (Map<String, String>) avatarObj;
                        avatarUrl = avatarMap.get("avatar_240");
                        if (avatarUrl == null) avatarUrl = avatarMap.get("avatar_72");
                    } else if (avatarObj instanceof String) {
                        avatarUrl = (String) avatarObj;
                    }
                }
                
                System.out.println("[syncFromPlatform] Syncing user: " + openId + " name=" + name);
                SystemUser user = getOrCreateUser(openId, name, avatarUrl);
                user.setDepartmentId(deptId);
                user.setDepartmentName(dept.getName());
                user.setLastSyncedAt(now());
                user.setUpdatedAt(now());
                userRepository.save(user);
            }
        }
        System.out.println("[syncFromPlatform] Sync complete. Total users: " + userRepository.count());
        return userRepository.findAll();
    }

    public void ensureSandboxUsers() {
        if (userRepository.count() == 0) {
            SystemUser zhang = getOrCreateUser("zhang_jingli", "张经理", null);
            zhang.setRole("ADMIN");
            zhang.setCanViewAll(true);
            userRepository.save(zhang);

            SystemUser li = getOrCreateUser("li_zhuguan", "李主管", null);
            li.setRole("USER");
            li.setDepartmentId("sandbox_dept_sales");
            userRepository.save(li);

            SystemUser wang = getOrCreateUser("wang_xiaoshou", "王销售", null);
            wang.setRole("USER");
            wang.setDepartmentId("sandbox_dept_sales");
            userRepository.save(wang);

            SystemDepartment salesDept = new SystemDepartment();
            salesDept.setDepartmentId("sandbox_dept_sales");
            salesDept.setName("销售部");
            salesDept.setLeaderUserId("li_zhuguan");
            salesDept.setParentId("sandbox_dept_root");
            departmentRepository.save(salesDept);
        }
    }

    public SystemUser updateRole(String platformUserId, String role, Boolean canViewAll) {
        SystemUser user = getOrCreateUser(platformUserId, platformUserId, null);
        user.setRole(role == null || role.isBlank() ? "USER" : role);
        user.setCanViewAll(Boolean.TRUE.equals(canViewAll) || "ADMIN".equalsIgnoreCase(role));
        user.setAdminSource("MANUAL");
        user.setUpdatedAt(now());
        return userRepository.save(user);
    }

    public SystemUser updateEnabled(String platformUserId, Boolean enabled) {
        SystemUser user = getOrCreateUser(platformUserId, platformUserId, null);
        user.setEnabled(Boolean.TRUE.equals(enabled));
        user.setStatus(Boolean.TRUE.equals(enabled) ? "ACTIVE" : "DISABLED");
        user.setUpdatedAt(now());
        return userRepository.save(user);
    }
}
