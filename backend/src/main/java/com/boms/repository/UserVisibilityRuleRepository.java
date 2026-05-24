package com.boms.repository;

import com.boms.model.UserVisibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserVisibilityRuleRepository extends JpaRepository<UserVisibilityRule, Long> {
    List<UserVisibilityRule> findByViewerUserIdAndCanViewTrue(String viewerUserId);
    List<UserVisibilityRule> findByViewerUserIdAndCanEditTrue(String viewerUserId);
    void deleteByViewerUserId(String viewerUserId);
}
