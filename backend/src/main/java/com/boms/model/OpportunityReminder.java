package com.boms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "opportunity_reminders")
public class OpportunityReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reminderType;
    private String targetUser;
    @Column(length = 5000)
    private String content;
    private String remindAt;
    private String status;
    private String sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    @JsonBackReference("opportunity-reminders")
    private Opportunity opportunity;

    public OpportunityReminder() {
    }

    public OpportunityReminder(String reminderType, String targetUser, String content, String remindAt, String status, String sentAt, Opportunity opportunity) {
        this.reminderType = reminderType;
        this.targetUser = targetUser;
        this.content = content;
        this.remindAt = remindAt;
        this.status = status;
        this.sentAt = sentAt;
        this.opportunity = opportunity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRemindAt() {
        return remindAt;
    }

    public void setRemindAt(String remindAt) {
        this.remindAt = remindAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }
}
