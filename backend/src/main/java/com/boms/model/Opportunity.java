package com.boms.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "opportunities")
public class Opportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String company;
    private String stage; // prospecting, qualification, proposal, negotiation, won, lost
    @Column(name = "opp_value")
    private Double value;
    private Integer probability;
    private String closeDate;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String source;
    private String priority; // low, medium, high
    private String owner;

    private String submitDate;
    private String submitter;
    private String govMarketManager;
    private String submitterRegion;
    private String industry;
    @Column(length = 2000)
    private String supplierCompany;

    @Column(length = 1000)
    private String deviceTypes;

    @Column(length = 1000)
    private String deviceModels;

    private Integer demandQuantity;
    private Double estimatedPurchaseAmount;
    private String estimatedPurchaseAmountUnit;
    private Double estimatedPurchaseAmountWan;
    private Boolean canPrepareParams;
    private String winRateLabel;
    private String supplyRegion;
    private Boolean requiresExclusiveAuthorization;

    @Column(length = 1000)
    private String authorizedCategories;

    private String expectedDeliveryDate;
    private String bidDeadline;
    private Boolean bidWon;
    private String purchaseType;
    private Double winningAmount;
    private String winningAmountUnit;
    private Double winningAmountWan;
    private Double deliBgDeliveryAmount;
    private String deliBgDeliveryAmountUnit;
    private Double deliBgDeliveryAmountWan;
    private String salesDepartment;
    private Boolean reportedSuccessfully;
    private String sales;
    private String creator;
    private String creatorUserId;
    private String creatorName;
    private String submitterUserId;
    private String submitterName;
    private String ownerUserId;
    private String ownerName;
    private String visibilityStatus;
    private String lastEditor;
    private String remindEditDate;
    private String lastEditDate;
    private String provinceGeneralManager;
    private String a4BusinessManager;
    private String a3BusinessManager;
    private String businessProgressStatus;
    private String reminderStatus;
    private Integer deviceRequirementVersion;
    private String deviceRequirementLockStatus;
    private String deviceRequirementLockedByFlowNo;
    private String deviceRequirementLockedAtNode;
    private String deviceRequirementLockedAt;
    private String reportFlowNo;
    private String reportFlowStatus;
    private String reportArchivedAt;
    private String bidDocumentFlowNo;
    private String bidDocumentFlowStatus;

    @Column(columnDefinition = "TEXT")
    private String weeklyProgress;

    @Column(columnDefinition = "TEXT")
    private String legacyExtraJson;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("opportunity-tasks")
    private List<TaskItem> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("opportunity-activities")
    private List<ActivityLog> activities = new ArrayList<>();

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("opportunity-attachments")
    private List<OpportunityAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("opportunity-reminders")
    private List<OpportunityReminder> reminders = new ArrayList<>();

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("opportunity-change-logs")
    private List<OpportunityChangeLog> changeLogs = new ArrayList<>();

    public Opportunity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Integer getProbability() {
        return probability;
    }

    public void setProbability(Integer probability) {
        this.probability = probability;
    }

    public String getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(String closeDate) {
        this.closeDate = closeDate;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(String submitDate) {
        this.submitDate = submitDate;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String getGovMarketManager() {
        return govMarketManager;
    }

    public void setGovMarketManager(String govMarketManager) {
        this.govMarketManager = govMarketManager;
    }

    public String getSubmitterRegion() {
        return submitterRegion;
    }

    public void setSubmitterRegion(String submitterRegion) {
        this.submitterRegion = submitterRegion;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getSupplierCompany() {
        return supplierCompany;
    }

    public void setSupplierCompany(String supplierCompany) {
        this.supplierCompany = supplierCompany;
    }

    public String getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(String deviceTypes) {
        this.deviceTypes = deviceTypes;
    }

    public String getDeviceModels() {
        return deviceModels;
    }

    public void setDeviceModels(String deviceModels) {
        this.deviceModels = deviceModels;
    }

    public Integer getDemandQuantity() {
        return demandQuantity;
    }

    public void setDemandQuantity(Integer demandQuantity) {
        this.demandQuantity = demandQuantity;
    }

    public Double getEstimatedPurchaseAmount() {
        return estimatedPurchaseAmount;
    }

    public void setEstimatedPurchaseAmount(Double estimatedPurchaseAmount) {
        this.estimatedPurchaseAmount = estimatedPurchaseAmount;
    }

    public String getEstimatedPurchaseAmountUnit() {
        return estimatedPurchaseAmountUnit;
    }

    public void setEstimatedPurchaseAmountUnit(String estimatedPurchaseAmountUnit) {
        this.estimatedPurchaseAmountUnit = estimatedPurchaseAmountUnit;
    }

    public Double getEstimatedPurchaseAmountWan() {
        return estimatedPurchaseAmountWan;
    }

    public void setEstimatedPurchaseAmountWan(Double estimatedPurchaseAmountWan) {
        this.estimatedPurchaseAmountWan = estimatedPurchaseAmountWan;
    }

    public Boolean getCanPrepareParams() {
        return canPrepareParams;
    }

    public void setCanPrepareParams(Boolean canPrepareParams) {
        this.canPrepareParams = canPrepareParams;
    }

    public String getWinRateLabel() {
        return winRateLabel;
    }

    public void setWinRateLabel(String winRateLabel) {
        this.winRateLabel = winRateLabel;
    }

    public String getSupplyRegion() {
        return supplyRegion;
    }

    public void setSupplyRegion(String supplyRegion) {
        this.supplyRegion = supplyRegion;
    }

    public Boolean getRequiresExclusiveAuthorization() {
        return requiresExclusiveAuthorization;
    }

    public void setRequiresExclusiveAuthorization(Boolean requiresExclusiveAuthorization) {
        this.requiresExclusiveAuthorization = requiresExclusiveAuthorization;
    }

    public String getAuthorizedCategories() {
        return authorizedCategories;
    }

    public void setAuthorizedCategories(String authorizedCategories) {
        this.authorizedCategories = authorizedCategories;
    }

    public String getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(String expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getBidDeadline() {
        return bidDeadline;
    }

    public void setBidDeadline(String bidDeadline) {
        this.bidDeadline = bidDeadline;
    }

    public Boolean getBidWon() {
        return bidWon;
    }

    public void setBidWon(Boolean bidWon) {
        this.bidWon = bidWon;
    }

    public String getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(String purchaseType) {
        this.purchaseType = purchaseType;
    }

    public Double getWinningAmount() {
        return winningAmount;
    }

    public void setWinningAmount(Double winningAmount) {
        this.winningAmount = winningAmount;
    }

    public String getWinningAmountUnit() {
        return winningAmountUnit;
    }

    public void setWinningAmountUnit(String winningAmountUnit) {
        this.winningAmountUnit = winningAmountUnit;
    }

    public Double getWinningAmountWan() {
        return winningAmountWan;
    }

    public void setWinningAmountWan(Double winningAmountWan) {
        this.winningAmountWan = winningAmountWan;
    }

    public Double getDeliBgDeliveryAmount() {
        return deliBgDeliveryAmount;
    }

    public void setDeliBgDeliveryAmount(Double deliBgDeliveryAmount) {
        this.deliBgDeliveryAmount = deliBgDeliveryAmount;
    }

    public String getDeliBgDeliveryAmountUnit() {
        return deliBgDeliveryAmountUnit;
    }

    public void setDeliBgDeliveryAmountUnit(String deliBgDeliveryAmountUnit) {
        this.deliBgDeliveryAmountUnit = deliBgDeliveryAmountUnit;
    }

    public Double getDeliBgDeliveryAmountWan() {
        return deliBgDeliveryAmountWan;
    }

    public void setDeliBgDeliveryAmountWan(Double deliBgDeliveryAmountWan) {
        this.deliBgDeliveryAmountWan = deliBgDeliveryAmountWan;
    }

    public String getSalesDepartment() {
        return salesDepartment;
    }

    public void setSalesDepartment(String salesDepartment) {
        this.salesDepartment = salesDepartment;
    }

    public Boolean getReportedSuccessfully() {
        return reportedSuccessfully;
    }

    public void setReportedSuccessfully(Boolean reportedSuccessfully) {
        this.reportedSuccessfully = reportedSuccessfully;
    }

    public String getSales() {
        return sales;
    }

    public void setSales(String sales) {
        this.sales = sales;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getSubmitterUserId() {
        return submitterUserId;
    }

    public void setSubmitterUserId(String submitterUserId) {
        this.submitterUserId = submitterUserId;
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public void setSubmitterName(String submitterName) {
        this.submitterName = submitterName;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getVisibilityStatus() {
        return visibilityStatus;
    }

    public void setVisibilityStatus(String visibilityStatus) {
        this.visibilityStatus = visibilityStatus;
    }

    public String getLastEditor() {
        return lastEditor;
    }

    public void setLastEditor(String lastEditor) {
        this.lastEditor = lastEditor;
    }

    public String getRemindEditDate() {
        return remindEditDate;
    }

    public void setRemindEditDate(String remindEditDate) {
        this.remindEditDate = remindEditDate;
    }

    public String getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(String lastEditDate) {
        this.lastEditDate = lastEditDate;
    }

    public String getProvinceGeneralManager() {
        return provinceGeneralManager;
    }

    public void setProvinceGeneralManager(String provinceGeneralManager) {
        this.provinceGeneralManager = provinceGeneralManager;
    }

    public String getA4BusinessManager() {
        return a4BusinessManager;
    }

    public void setA4BusinessManager(String a4BusinessManager) {
        this.a4BusinessManager = a4BusinessManager;
    }

    public String getA3BusinessManager() {
        return a3BusinessManager;
    }

    public void setA3BusinessManager(String a3BusinessManager) {
        this.a3BusinessManager = a3BusinessManager;
    }

    public String getBusinessProgressStatus() {
        return businessProgressStatus;
    }

    public void setBusinessProgressStatus(String businessProgressStatus) {
        this.businessProgressStatus = businessProgressStatus;
    }

    public String getReminderStatus() {
        return reminderStatus;
    }

    public void setReminderStatus(String reminderStatus) {
        this.reminderStatus = reminderStatus;
    }

    public Integer getDeviceRequirementVersion() {
        return deviceRequirementVersion;
    }

    public void setDeviceRequirementVersion(Integer deviceRequirementVersion) {
        this.deviceRequirementVersion = deviceRequirementVersion;
    }

    public String getDeviceRequirementLockStatus() {
        return deviceRequirementLockStatus;
    }

    public void setDeviceRequirementLockStatus(String deviceRequirementLockStatus) {
        this.deviceRequirementLockStatus = deviceRequirementLockStatus;
    }

    public String getDeviceRequirementLockedByFlowNo() {
        return deviceRequirementLockedByFlowNo;
    }

    public void setDeviceRequirementLockedByFlowNo(String deviceRequirementLockedByFlowNo) {
        this.deviceRequirementLockedByFlowNo = deviceRequirementLockedByFlowNo;
    }

    public String getDeviceRequirementLockedAtNode() {
        return deviceRequirementLockedAtNode;
    }

    public void setDeviceRequirementLockedAtNode(String deviceRequirementLockedAtNode) {
        this.deviceRequirementLockedAtNode = deviceRequirementLockedAtNode;
    }

    public String getDeviceRequirementLockedAt() {
        return deviceRequirementLockedAt;
    }

    public void setDeviceRequirementLockedAt(String deviceRequirementLockedAt) {
        this.deviceRequirementLockedAt = deviceRequirementLockedAt;
    }

    public String getReportFlowNo() {
        return reportFlowNo;
    }

    public void setReportFlowNo(String reportFlowNo) {
        this.reportFlowNo = reportFlowNo;
    }

    public String getReportFlowStatus() {
        return reportFlowStatus;
    }

    public void setReportFlowStatus(String reportFlowStatus) {
        this.reportFlowStatus = reportFlowStatus;
    }

    public String getReportArchivedAt() {
        return reportArchivedAt;
    }

    public void setReportArchivedAt(String reportArchivedAt) {
        this.reportArchivedAt = reportArchivedAt;
    }

    public String getBidDocumentFlowNo() {
        return bidDocumentFlowNo;
    }

    public void setBidDocumentFlowNo(String bidDocumentFlowNo) {
        this.bidDocumentFlowNo = bidDocumentFlowNo;
    }

    public String getBidDocumentFlowStatus() {
        return bidDocumentFlowStatus;
    }

    public void setBidDocumentFlowStatus(String bidDocumentFlowStatus) {
        this.bidDocumentFlowStatus = bidDocumentFlowStatus;
    }

    public String getWeeklyProgress() {
        return weeklyProgress;
    }

    public void setWeeklyProgress(String weeklyProgress) {
        this.weeklyProgress = weeklyProgress;
    }

    public String getLegacyExtraJson() {
        return legacyExtraJson;
    }

    public void setLegacyExtraJson(String legacyExtraJson) {
        this.legacyExtraJson = legacyExtraJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TaskItem> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskItem> tasks) {
        this.tasks.clear();
        if (tasks != null) {
            for (TaskItem t : tasks) {
                t.setOpportunity(this);
                this.tasks.add(t);
            }
        }
    }

    public List<ActivityLog> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityLog> activities) {
        this.activities.clear();
        if (activities != null) {
            for (ActivityLog a : activities) {
                a.setOpportunity(this);
                this.activities.add(a);
            }
        }
    }

    public List<OpportunityAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<OpportunityAttachment> attachments) {
        this.attachments.clear();
        if (attachments != null) {
            for (OpportunityAttachment attachment : attachments) {
                attachment.setOpportunity(this);
                this.attachments.add(attachment);
            }
        }
    }

    public List<OpportunityReminder> getReminders() {
        return reminders;
    }

    public void setReminders(List<OpportunityReminder> reminders) {
        this.reminders.clear();
        if (reminders != null) {
            for (OpportunityReminder reminder : reminders) {
                reminder.setOpportunity(this);
                this.reminders.add(reminder);
            }
        }
    }

    public List<OpportunityChangeLog> getChangeLogs() {
        return changeLogs;
    }

    public void setChangeLogs(List<OpportunityChangeLog> changeLogs) {
        this.changeLogs.clear();
        if (changeLogs != null) {
            for (OpportunityChangeLog log : changeLogs) {
                log.setOpportunity(this);
                this.changeLogs.add(log);
            }
        }
    }
}
