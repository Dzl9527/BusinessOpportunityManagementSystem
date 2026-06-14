package com.boms.dto;

import com.alibaba.excel.annotation.ExcelProperty;

public class OpportunityExportDTO {

    @ExcelProperty("商机名称")
    private String name;

    @ExcelProperty("采购单位")
    private String company;

    @ExcelProperty("商机阶段")
    private String stage;

    @ExcelProperty("预估金额")
    private Double value;

    @ExcelProperty("赢率(%)")
    private Integer probability;

    @ExcelProperty("预计成交日期")
    private String closeDate;

    @ExcelProperty("负责人")
    private String owner;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("商机来源")
    private String source;

    @ExcelProperty("联系人")
    private String contactName;

    @ExcelProperty("联系电话")
    private String contactPhone;

    @ExcelProperty("行业")
    private String industry;

    @ExcelProperty("提报人")
    private String submitter;

    @ExcelProperty("提报时间")
    private String submitDate;

    @ExcelProperty("供货省区")
    private String supplyRegion;

    @ExcelProperty("业务进度")
    private String businessProgressStatus;

    @ExcelProperty("是否赢单")
    private String bidWonStr;

    @ExcelProperty("商机描述")
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public Integer getProbability() { return probability; }
    public void setProbability(Integer probability) { this.probability = probability; }
    public String getCloseDate() { return closeDate; }
    public void setCloseDate(String closeDate) { this.closeDate = closeDate; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }
    public String getSubmitDate() { return submitDate; }
    public void setSubmitDate(String submitDate) { this.submitDate = submitDate; }
    public String getSupplyRegion() { return supplyRegion; }
    public void setSupplyRegion(String supplyRegion) { this.supplyRegion = supplyRegion; }
    public String getBusinessProgressStatus() { return businessProgressStatus; }
    public void setBusinessProgressStatus(String businessProgressStatus) { this.businessProgressStatus = businessProgressStatus; }
    public String getBidWonStr() { return bidWonStr; }
    public void setBidWonStr(String bidWonStr) { this.bidWonStr = bidWonStr; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
