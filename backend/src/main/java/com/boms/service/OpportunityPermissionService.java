package com.boms.service;

import com.boms.model.Opportunity;
import com.boms.model.SystemUser;
import com.boms.model.UserVisibilityRule;
import com.boms.repository.UserVisibilityRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OpportunityPermissionService {
    @Autowired
    private UserVisibilityRuleRepository visibilityRuleRepository;

    public Set<String> getVisibleUserIds(SystemUser user) {
        Set<String> ids = new HashSet<>();
        if (user == null || user.getWecomUserId() == null) {
            return ids;
        }
        ids.add(user.getWecomUserId());
        for (UserVisibilityRule rule : visibilityRuleRepository.findByViewerUserIdAndCanViewTrue(user.getWecomUserId())) {
            ids.add(rule.getVisibleUserId());
        }
        return ids;
    }

    public Set<String> getEditableUserIds(SystemUser user) {
        Set<String> ids = new HashSet<>();
        if (user == null || user.getWecomUserId() == null) {
            return ids;
        }
        ids.add(user.getWecomUserId());
        for (UserVisibilityRule rule : visibilityRuleRepository.findByViewerUserIdAndCanEditTrue(user.getWecomUserId())) {
            ids.add(rule.getVisibleUserId());
        }
        return ids;
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
        if (user != null && user.getWecomUserId() != null) {
            self.add(user.getWecomUserId());
        }
        if (belongsToAny(opp, self)) {
            return "SELF";
        }
        return "WHITELIST";
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
