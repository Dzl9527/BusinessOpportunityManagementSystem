package com.boms.service;

import com.boms.model.Opportunity;
import com.boms.model.SystemDepartment;
import com.boms.model.SystemUser;
import com.boms.repository.SystemDepartmentRepository;
import com.boms.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpportunityPermissionService {

    @Autowired
    private SystemDepartmentRepository departmentRepository;

    @Autowired
    private SystemUserRepository userRepository;

    public Set<String> getVisibleUserIds(SystemUser user) {
        Set<String> ids = new HashSet<>();
        if (user == null || user.getPlatformUserId() == null) {
            return ids;
        }
        ids.add(user.getPlatformUserId());
        
        // Find all departments where user is leader
        List<SystemDepartment> allDepts = departmentRepository.findAll();
        Set<String> leaderOfDeptIds = new HashSet<>();
        for (SystemDepartment dept : allDepts) {
            if (user.getPlatformUserId().equals(dept.getLeaderUserId())) {
                leaderOfDeptIds.add(dept.getDepartmentId());
            }
        }
        
        if (!leaderOfDeptIds.isEmpty()) {
            Set<String> subordinateDeptIds = new HashSet<>(leaderOfDeptIds);
            boolean added = true;
            while (added) {
                added = false;
                for (SystemDepartment dept : allDepts) {
                    if (subordinateDeptIds.contains(dept.getParentId()) && !subordinateDeptIds.contains(dept.getDepartmentId())) {
                        subordinateDeptIds.add(dept.getDepartmentId());
                        added = true;
                    }
                }
            }
            
            List<SystemUser> allUsers = userRepository.findAll();
            for (SystemUser subordinate : allUsers) {
                if (subordinateDeptIds.contains(subordinate.getDepartmentId())) {
                    ids.add(subordinate.getPlatformUserId());
                }
            }
        }
        return ids;
    }

    public Set<String> getEditableUserIds(SystemUser user) {
        // By default, editable equals visible in this new model, or just self + admins
        return getVisibleUserIds(user);
    }

    public boolean canView(SystemUser user, Opportunity opp) {
        if (user == null || opp == null || !user.isActive()) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        if ("UNASSIGNED".equalsIgnoreCase(opp.getVisibilityStatus())) {
            return false;
        }
        return belongsToAny(opp, getVisibleUserIds(user));
    }

    public boolean canEdit(SystemUser user, Opportunity opp) {
        if (user == null || opp == null || !user.isActive()) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        if ("UNASSIGNED".equalsIgnoreCase(opp.getVisibilityStatus())) {
            return false;
        }
        return belongsToAny(opp, getEditableUserIds(user));
    }

    public String permissionSource(SystemUser user, Opportunity opp) {
        if (user != null && user.isAdmin()) {
            return "ADMIN";
        }
        Set<String> self = new HashSet<>();
        if (user != null && user.getPlatformUserId() != null) {
            self.add(user.getPlatformUserId());
        }
        if (belongsToAny(opp, self)) {
            return "SELF";
        }
        return "ORGANIZATION_TREE";
    }

    public List<Opportunity> filterVisible(SystemUser user, List<Opportunity> opportunities) {
        return opportunities.stream().filter(opp -> canView(user, opp)).collect(Collectors.toList());
    }

    private boolean belongsToAny(Opportunity opp, Set<String> userIds) {
        return userIds.stream().anyMatch(userId ->
                Objects.equals(userId, opp.getCreatorUserId())
                        || Objects.equals(userId, opp.getSubmitterUserId())
                        || Objects.equals(userId, opp.getOwnerUserId()));
    }
}
