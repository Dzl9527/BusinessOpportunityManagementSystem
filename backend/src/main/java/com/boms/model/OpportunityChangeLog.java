package com.boms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "opportunity_change_logs")
public class OpportunityChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    @JsonBackReference("opportunity-change-logs")
    private Opportunity opportunity;

    private String editorUserId;
    private String editorName;
    private String editedAt;
    private String fieldName;
    private String fieldLabel;

    @Column(length = 3000)
    private String oldValue;

    @Column(length = 3000)
    private String newValue;

    private String source;
    private String permissionSource;
    private String ownerUserId;

    public OpportunityChangeLog() {
    }

    public OpportunityChangeLog(Opportunity opportunity, String editorUserId, String editorName, String editedAt,
                                String fieldName, String fieldLabel, String oldValue, String newValue,
                                String source, String permissionSource, String ownerUserId) {
        this.opportunity = opportunity;
        this.editorUserId = editorUserId;
        this.editorName = editorName;
        this.editedAt = editedAt;
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.source = source;
        this.permissionSource = permissionSource;
        this.ownerUserId = ownerUserId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public String getEditorUserId() {
        return editorUserId;
    }

    public void setEditorUserId(String editorUserId) {
        this.editorUserId = editorUserId;
    }

    public String getEditorName() {
        return editorName;
    }

    public void setEditorName(String editorName) {
        this.editorName = editorName;
    }

    public String getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(String editedAt) {
        this.editedAt = editedAt;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPermissionSource() {
        return permissionSource;
    }

    public void setPermissionSource(String permissionSource) {
        this.permissionSource = permissionSource;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
