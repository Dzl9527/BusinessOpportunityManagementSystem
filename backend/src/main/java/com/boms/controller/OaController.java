package com.boms.controller;

import com.boms.model.ActivityLog;
import com.boms.model.Opportunity;
import com.boms.model.SystemUser;
import com.boms.repository.OpportunityRepository;
import com.boms.service.OpportunityAuditService;
import com.boms.service.OpportunityPermissionService;
import com.boms.service.UserDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/oa")
@CrossOrigin(origins = "*")
public class OaController {
    @Autowired
    private OpportunityRepository oppRepository;

    @Autowired
    private UserDirectoryService userDirectoryService;

    @Autowired
    private OpportunityPermissionService permissionService;

    @Autowired
    private OpportunityAuditService auditService;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String getString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String requireString(Map<String, Object> body, String key) {
        String value = getString(body, key);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须传入" + key);
        }
        return value;
    }

    private Integer getInteger(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " 必须是数字");
        }
    }

    private SystemUser resolveUser(Map<String, Object> body, String fallbackName) {
        String userId = Optional.ofNullable(getString(body, "userId")).orElse("oa_gateway");
        String userName = Optional.ofNullable(getString(body, "userName")).orElse(fallbackName);
        return userDirectoryService.getCurrentUser(userId, userName);
    }

    private Opportunity findByReportFlowNo(String reportFlowNo) {
        if (reportFlowNo == null || reportFlowNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须传入reportFlowNo");
        }
        return oppRepository.findByReportFlowNo(reportFlowNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到商机报备流程"));
    }

    private Opportunity findByBidDocumentFlowNo(String bidDocumentFlowNo) {
        if (bidDocumentFlowNo == null || bidDocumentFlowNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须传入bidDocumentFlowNo");
        }
        return oppRepository.findByBidDocumentFlowNo(bidDocumentFlowNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到项目授权流程"));
    }

    private void ensureOaDefaults(Opportunity opp) {
        if (opp.getDeviceRequirementVersion() == null || opp.getDeviceRequirementVersion() < 1) {
            opp.setDeviceRequirementVersion(1);
        }
        if (opp.getDeviceRequirementLockStatus() == null || opp.getDeviceRequirementLockStatus().isBlank()) {
            opp.setDeviceRequirementLockStatus("UNLOCKED");
        }
        if (opp.getReportFlowStatus() == null || opp.getReportFlowStatus().isBlank()) {
            opp.setReportFlowStatus("NOT_STARTED");
        }
        if (opp.getBidDocumentFlowStatus() == null || opp.getBidDocumentFlowStatus().isBlank()) {
            opp.setBidDocumentFlowStatus("NOT_STARTED");
        }
    }

    private boolean hasActiveOrArchivedReportFlow(Opportunity opp) {
        return "IN_PROGRESS".equalsIgnoreCase(opp.getReportFlowStatus())
                || "ARCHIVED".equalsIgnoreCase(opp.getReportFlowStatus());
    }

    private void addActivity(Opportunity opp, String content) {
        opp.getActivities().add(0, new ActivityLog("system", content, now(), opp));
    }

    private Map<String, Object> prefillPayload(Opportunity opp) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("opportunityId", opp.getId());
        payload.put("reportFlowNo", opp.getReportFlowNo());
        payload.put("company", opp.getCompany());
        payload.put("name", opp.getName());
        payload.put("supplierCompany", opp.getSupplierCompany());
        payload.put("industry", opp.getIndustry());
        payload.put("deviceTypes", opp.getDeviceTypes());
        payload.put("deviceModels", opp.getDeviceModels());
        payload.put("estimatedPurchaseAmount", opp.getEstimatedPurchaseAmount());
        payload.put("estimatedPurchaseAmountUnit", opp.getEstimatedPurchaseAmountUnit());
        payload.put("requiresExclusiveAuthorization", opp.getRequiresExclusiveAuthorization());
        payload.put("authorizedCategories", opp.getAuthorizedCategories());
        payload.put("canPrepareParams", opp.getCanPrepareParams());
        payload.put("expectedDeliveryDate", opp.getExpectedDeliveryDate());
        payload.put("bidDeadline", opp.getBidDeadline());
        payload.put("submitterName", opp.getSubmitterName() != null ? opp.getSubmitterName() : opp.getSubmitter());
        payload.put("submitterUserId", opp.getSubmitterUserId());
        payload.put("deviceRequirementVersion", opp.getDeviceRequirementVersion());
        payload.put("deviceRequirementLockStatus", opp.getDeviceRequirementLockStatus());
        payload.put("lastEditDate", opp.getLastEditDate());
        payload.put("editableInOa", false);
        return payload;
    }

    @PostMapping("/opportunities/{opportunityId}/report-flow/start")
    public Opportunity startReportFlow(@PathVariable Long opportunityId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> safeBody = body == null ? new LinkedHashMap<>() : body;
        SystemUser currentUser = resolveUser(safeBody, "OA发起人");
        Opportunity opp = oppRepository.findById(opportunityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商机不存在"));
        if (!permissionService.canEdit(currentUser, opp)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权发起该商机的OA报备流程");
        }
        ensureOaDefaults(opp);
        if (hasActiveOrArchivedReportFlow(opp)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该商机已存在商机报备流程");
        }
        Map<String, String> before = auditService.snapshot(opp);
        String reportFlowNo = getString(safeBody, "reportFlowNo");
        if (reportFlowNo == null || reportFlowNo.isBlank()) {
            reportFlowNo = "MOCK-BB-" + opp.getId() + "-" + System.currentTimeMillis();
        }
        opp.setReportFlowNo(reportFlowNo);
        opp.setReportFlowStatus("IN_PROGRESS");
        addActivity(opp, "已发起OA商机报备流程，流程号：" + reportFlowNo);
        Opportunity saved = oppRepository.save(opp);
        auditService.recordChanges(saved, before, currentUser, "OA", permissionService.permissionSource(currentUser, saved));
        return saved;
    }

    @PostMapping("/callback/report-flow/archived")
    public Opportunity archiveReportFlow(@RequestBody Map<String, Object> body) {
        Integer oppIdValue = getInteger(body, "opportunityId");
        Long opportunityId = oppIdValue == null ? null : Long.valueOf(oppIdValue);
        Opportunity opp;
        if (opportunityId != null) {
            opp = oppRepository.findById(opportunityId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "商机不存在"));
        } else {
            opp = findByReportFlowNo(requireString(body, "reportFlowNo"));
        }
        ensureOaDefaults(opp);
        Map<String, String> before = auditService.snapshot(opp);
        String reportFlowNo = getString(body, "reportFlowNo");
        if (reportFlowNo != null && !reportFlowNo.isBlank()) {
            opp.setReportFlowNo(reportFlowNo);
        }
        String status = Optional.ofNullable(getString(body, "status")).orElse("ARCHIVED");
        opp.setReportFlowStatus(status);
        opp.setReportArchivedAt(Optional.ofNullable(getString(body, "archivedAt")).orElse(now()));
        addActivity(opp, "OA商机报备流程已归档，流程号：" + opp.getReportFlowNo());
        Opportunity saved = oppRepository.save(opp);
        auditService.recordChanges(saved, before, null, "OA", "SYSTEM");
        return saved;
    }

    @GetMapping("/report-flows/{reportFlowNo}/bid-document-prefill")
    public Map<String, Object> bidDocumentPrefill(@PathVariable String reportFlowNo) {
        Opportunity opp = findByReportFlowNo(reportFlowNo);
        ensureOaDefaults(opp);
        if (!"ARCHIVED".equalsIgnoreCase(opp.getReportFlowStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "商机报备流程未归档");
        }
        return prefillPayload(opp);
    }

    @PostMapping("/callback/bid-document-flow/started")
    public Opportunity startBidDocumentFlow(@RequestBody Map<String, Object> body) {
        Opportunity opp = findByReportFlowNo(requireString(body, "reportFlowNo"));
        ensureOaDefaults(opp);
        if (!"ARCHIVED".equalsIgnoreCase(opp.getReportFlowStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "商机报备流程未归档");
        }
        if ("HARD_LOCKED".equalsIgnoreCase(opp.getDeviceRequirementLockStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "需求设备类型已硬锁定");
        }
        Integer requestVersion = getInteger(body, "deviceRequirementVersion");
        if (requestVersion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须传入deviceRequirementVersion");
        }
        if (!requestVersion.equals(opp.getDeviceRequirementVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "商机数据版本已变化，请重新同步最新数据");
        }
        Map<String, String> before = auditService.snapshot(opp);
        String bidDocumentFlowNo = requireString(body, "bidDocumentFlowNo");
        opp.setBidDocumentFlowNo(bidDocumentFlowNo);
        opp.setBidDocumentFlowStatus("IN_PROGRESS");
        opp.setDeviceRequirementLockStatus("SOFT_LOCKED");
        opp.setDeviceRequirementLockedByFlowNo(bidDocumentFlowNo);
        opp.setDeviceRequirementLockedAt(Optional.ofNullable(getString(body, "startedAt")).orElse(now()));
        addActivity(opp, "OA项目授权流程已发起，需求设备类型临时冻结，流程号：" + bidDocumentFlowNo);
        Opportunity saved = oppRepository.save(opp);
        auditService.recordChanges(saved, before, null, "OA", "SYSTEM");
        return saved;
    }

    @PostMapping("/callback/bid-document-flow/node-reached")
    public Opportunity bidDocumentNodeReached(@RequestBody Map<String, Object> body) {
        Opportunity opp = findByBidDocumentFlowNo(requireString(body, "bidDocumentFlowNo"));
        ensureOaDefaults(opp);
        Map<String, String> before = auditService.snapshot(opp);
        String nodeCode = getString(body, "nodeCode");
        if ("06".equals(nodeCode)) {
            opp.setBidDocumentFlowStatus("NODE_06_REACHED");
            opp.setDeviceRequirementLockStatus("HARD_LOCKED");
            opp.setDeviceRequirementLockedAtNode("06");
            opp.setDeviceRequirementLockedAt(Optional.ofNullable(getString(body, "arrivedAt")).orElse(now()));
            addActivity(opp, "OA项目授权流程到达授权书创建审批06节点，需求设备类型已硬锁定。");
        }
        Opportunity saved = oppRepository.save(opp);
        auditService.recordChanges(saved, before, null, "OA", "SYSTEM");
        return saved;
    }

    @PostMapping("/callback/bid-document-flow/status-changed")
    public Opportunity bidDocumentStatusChanged(@RequestBody Map<String, Object> body) {
        Opportunity opp = findByBidDocumentFlowNo(requireString(body, "bidDocumentFlowNo"));
        ensureOaDefaults(opp);
        Map<String, String> before = auditService.snapshot(opp);
        String status = getString(body, "status");
        opp.setBidDocumentFlowStatus(status);
        if ("NODE_06_REACHED".equalsIgnoreCase(status)) {
            opp.setDeviceRequirementLockStatus("HARD_LOCKED");
            opp.setDeviceRequirementLockedAtNode("06");
            opp.setDeviceRequirementLockedAt(Optional.ofNullable(getString(body, "changedAt")).orElse(now()));
            addActivity(opp, "OA项目授权流程到达授权书创建审批06节点，需求设备类型已硬锁定。");
        } else if (("REJECTED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status))
                && "SOFT_LOCKED".equalsIgnoreCase(opp.getDeviceRequirementLockStatus())) {
            opp.setDeviceRequirementLockStatus("UNLOCKED");
            opp.setDeviceRequirementLockedByFlowNo(null);
            opp.setDeviceRequirementLockedAtNode(null);
            opp.setDeviceRequirementLockedAt(null);
            addActivity(opp, "OA项目授权流程" + status + "，已解除需求设备类型临时冻结。");
        } else {
            addActivity(opp, "OA项目授权流程状态变更为：" + status);
        }
        Opportunity saved = oppRepository.save(opp);
        auditService.recordChanges(saved, before, null, "OA", "SYSTEM");
        return saved;
    }
}
