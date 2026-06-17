package com.boms.controller;

import com.boms.model.SystemDepartment;
import com.boms.repository.SystemDepartmentRepository;
import com.boms.service.UserDirectoryService;
import com.boms.model.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private SystemDepartmentRepository departmentRepository;

    @Autowired
    private UserDirectoryService userDirectoryService;

    private boolean isAdmin(String adminUserId, String adminName) {
        String securityId = com.boms.security.SecurityUtils.getCurrentUserId();
        if (securityId == null) securityId = adminUserId;
        String resolvedName = adminName == null || adminName.trim().isEmpty() ? "未登录用户" : adminName.trim();
        SystemUser user = userDirectoryService.getCurrentUser(securityId, resolvedName);
        return user != null && user.isActive() && user.isAdmin();
    }

    @GetMapping
    public ResponseEntity<List<SystemDepartment>> listDepartments(@RequestParam(required = false) String adminUserId,
                                                                  @RequestParam(required = false) String adminName) {
        if (!isAdmin(adminUserId, adminName)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(departmentRepository.findAll());
    }
}
