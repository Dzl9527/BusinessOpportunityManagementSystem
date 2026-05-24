package com.boms.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "activities")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // system, meeting, call, email
    
    @Column(length = 2000)
    private String content;
    
    private String date; // YYYY-MM-DD HH:mm

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    @JsonBackReference("opportunity-activities")
    private Opportunity opportunity;

    public ActivityLog() {
    }

    public ActivityLog(String type, String content, String date, Opportunity opportunity) {
        this.type = type;
        this.content = content;
        this.date = date;
        this.opportunity = opportunity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Opportunity getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }
}
