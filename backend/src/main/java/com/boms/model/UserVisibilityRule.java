package com.boms.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "user_visibility_rules",
        uniqueConstraints = @UniqueConstraint(columnNames = {"viewer_user_id", "visible_user_id"})
)
public class UserVisibilityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_user_id", nullable = false, length = 100)
    private String viewerUserId;

    @Column(name = "visible_user_id", nullable = false, length = 100)
    private String visibleUserId;

    private Boolean canView;
    private Boolean canEdit;
    private String createdByUserId;
    private String createdByName;
    private String createdAt;
    private String updatedAt;

    public UserVisibilityRule() {
    }

    public UserVisibilityRule(String viewerUserId, String visibleUserId, Boolean canView, Boolean canEdit) {
        this.viewerUserId = viewerUserId;
        this.visibleUserId = visibleUserId;
        this.canView = canView;
        this.canEdit = canEdit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getViewerUserId() {
        return viewerUserId;
    }

    public void setViewerUserId(String viewerUserId) {
        this.viewerUserId = viewerUserId;
    }

    public String getVisibleUserId() {
        return visibleUserId;
    }

    public void setVisibleUserId(String visibleUserId) {
        this.visibleUserId = visibleUserId;
    }

    public Boolean getCanView() {
        return canView;
    }

    public void setCanView(Boolean canView) {
        this.canView = canView;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
