package com.boms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "system_users")
public class SystemUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String wecomUserId;

    @Column(nullable = false, length = 100)
    private String name;

    private String mobile;
    private String email;
    private String departmentId;
    private String departmentName;
    private String position;
    private String role;
    private Boolean canViewAll;
    private Boolean enabled;
    private String status;
    private String adminSource;
    private String lastSyncedAt;
    private String createdAt;
    private String updatedAt;

    public SystemUser() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWecomUserId() {
        return wecomUserId;
    }

    public void setWecomUserId(String wecomUserId) {
        this.wecomUserId = wecomUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getCanViewAll() {
        return canViewAll;
    }

    public void setCanViewAll(Boolean canViewAll) {
        this.canViewAll = canViewAll;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminSource() {
        return adminSource;
    }

    public void setAdminSource(String adminSource) {
        this.adminSource = adminSource;
    }

    public String getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(String lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || Boolean.TRUE.equals(canViewAll);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(enabled) && !"LEFT".equalsIgnoreCase(status) && !"DISABLED".equalsIgnoreCase(status);
    }
}
