package com.boms.controller;

import com.boms.model.ActivityLog;
import com.boms.model.Opportunity;
import com.boms.model.OpportunityAttachment;
import com.boms.model.OpportunityChangeLog;
import com.boms.model.OpportunityReminder;
import com.boms.model.SystemUser;
import com.boms.model.TaskItem;
import com.boms.repository.OpportunityRepository;
import com.boms.service.OpportunityAuditService;
import com.boms.service.OpportunityPermissionService;
import com.boms.service.UserDirectoryService;
import com.boms.service.FeishuService;
import com.boms.service.OpportunityReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.boms.specification.OpportunitySpec;
import com.boms.dto.OpportunityExportDTO;
import com.alibaba.excel.EasyExcel;

@RestController
@RequestMapping("/api/opportunities")
@CrossOrigin(origins = "*")
public class OpportunityController {

    @Autowired
    private OpportunityRepository oppRepository;

    @Autowired
    private FeishuService feishuService;

    @Autowired
    private UserDirectoryService userDirectoryService;

    @Autowired
    private OpportunityPermissionService permissionService;

    @Autowired
    private OpportunityAuditService auditService;

    @Autowired
    private OpportunityReminderService reminderService;

    private static final Map<String, String> STAGES = new LinkedHashMap<>() {{
        put("prospecting", "发现商机");
        put("qualification", "资质评估");
        put("proposal", "方案报价");
        put("negotiation", "谈判协商");
        put("won", "赢得商机");
        put("lost", "流失商机");
    }};

    private static final Map<String, Integer> WIN_RATE_VALUES = new LinkedHashMap<>() {{
        put("10%-没有把握", 10);
        put("30%-把握很小", 30);
        put("50%-可以参与", 50);
        put("70%-赢面很大", 70);
        put("100%-肯定中标", 100);
    }};

    private static final Map<String, Integer> DEFAULT_PROBABILITIES = new HashMap<>() {{
        put("prospecting", 20);
        put("qualification", 40);
        put("proposal", 60);
        put("negotiation", 80);
        put("won", 100);
        put("lost", 0);
    }};

    private static final Set<String> DEVICE_REQUIREMENT_VERSION_FIELDS = Set.of(
            "deviceTypes",
            "deviceModels",
            "requiresExclusiveAuthorization",
            "authorizedCategories",
            "canPrepareParams",
            "supplierCompany",
            "estimatedPurchaseAmount",
            "estimatedPurchaseAmountUnit",
            "bidDeadline"
    );

    private String getNowString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String getTodayString() {
        return LocalDate.now().toString();
    }

    private String getCurrentUserId(String userId) {
        String securityId = com.boms.security.SecurityUtils.getCurrentUserId();
        return securityId != null ? securityId : "anonymous";
    }

    private String getCurrentUserName(String userName) {
        // Fallback or lookup from directory could happen here, 
        // but typically user is loaded via userId now.
        return userName == null || userName.trim().isEmpty() ? "未登录用户" : userName.trim();
    }

    private boolean containsIgnoreCase(String text, String query) {
        return text != null && query != null && text.toLowerCase().contains(query.toLowerCase());
    }

    private SystemUser resolveCurrentUser(String userId, String userName) {
        return userDirectoryService.getCurrentUser(getCurrentUserId(userId), getCurrentUserName(userName));
    }

    private boolean equalsParam(String value, String param) {
        return param == null || param.equals("all") || param.trim().isEmpty() || Objects.equals(value, param);
    }

    private List<Opportunity> getVisibleOpportunities(String userId, String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return permissionService.filterVisible(currentUser, oppRepository.findAll());
    }

    private Map<String, Double> initFunnelMap() {
        Map<String, Double> funnel = new LinkedHashMap<>();
        for (String key : STAGES.keySet()) {
            funnel.put(key, 0.0);
        }
        return funnel;
    }

    private Map<String, Long> initStageCountMap() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : STAGES.keySet()) {
            counts.put(key, 0L);
        }
        return counts;
    }

    private boolean equalsBooleanParam(Boolean value, String param) {
        if (param == null || param.equals("all") || param.trim().isEmpty()) {
            return true;
        }
        if ("true".equalsIgnoreCase(param) || "是".equals(param)) {
            return Boolean.TRUE.equals(value);
        }
        if ("false".equalsIgnoreCase(param) || "否".equals(param)) {
            return Boolean.FALSE.equals(value);
        }
        return true;
    }

    private boolean isDeviceTypeLocked(Opportunity opp) {
        return "SOFT_LOCKED".equalsIgnoreCase(opp.getDeviceRequirementLockStatus())
                || "HARD_LOCKED".equalsIgnoreCase(opp.getDeviceRequirementLockStatus());
    }

    private boolean isDeviceTypeChanged(Opportunity opp, Opportunity updatedData) {
        return !Objects.equals(opp.getDeviceTypes(), updatedData.getDeviceTypes());
    }

    private int nextDeviceRequirementVersion(Opportunity opp) {
        return Optional.ofNullable(opp.getDeviceRequirementVersion()).orElse(1) + 1;
    }

    private boolean hasDeviceRequirementVersionChange(Map<String, String> before, Opportunity opp) {
        Map<String, String> after = auditService.snapshot(opp);
        return DEVICE_REQUIREMENT_VERSION_FIELDS.stream()
                .anyMatch(field -> !Objects.equals(before.get(field), after.get(field)));
    }

    private double toYuan(Double amount, String unit) {
        if (amount == null) {
            return 0.0;
        }
        if ("万元".equals(unit)) {
            return amount * 10000;
        }
        return amount;
    }

    private void normalizeOpportunity(Opportunity opp, String userName, boolean creating) {
        if (opp.getStage() == null || opp.getStage().isBlank()) {
            opp.setStage("prospecting");
        }
        if (opp.getPriority() == null || opp.getPriority().isBlank()) {
            opp.setPriority("medium");
        }
        if (opp.getBusinessProgressStatus() == null || opp.getBusinessProgressStatus().isBlank()) {
            opp.setBusinessProgressStatus("新提报");
        }
        if (opp.getReminderStatus() == null || opp.getReminderStatus().isBlank()) {
            opp.setReminderStatus("未提醒");
        }
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
        if (opp.getEstimatedPurchaseAmountUnit() == null || opp.getEstimatedPurchaseAmountUnit().isBlank()) {
            opp.setEstimatedPurchaseAmountUnit("万元");
        }
        if (opp.getWinningAmountUnit() == null || opp.getWinningAmountUnit().isBlank()) {
            opp.setWinningAmountUnit(opp.getEstimatedPurchaseAmountUnit());
        }
        if (opp.getDeliBgDeliveryAmountUnit() == null || opp.getDeliBgDeliveryAmountUnit().isBlank()) {
            opp.setDeliBgDeliveryAmountUnit(opp.getEstimatedPurchaseAmountUnit());
        }
        if (opp.getWinRateLabel() != null && WIN_RATE_VALUES.containsKey(opp.getWinRateLabel())) {
            opp.setProbability(WIN_RATE_VALUES.get(opp.getWinRateLabel()));
        } else if (opp.getProbability() == null) {
            opp.setProbability(DEFAULT_PROBABILITIES.getOrDefault(opp.getStage(), 20));
        }
        if (opp.getEstimatedPurchaseAmount() != null) {
            opp.setValue(toYuan(opp.getEstimatedPurchaseAmount(), opp.getEstimatedPurchaseAmountUnit()));
            if ("万元".equals(opp.getEstimatedPurchaseAmountUnit())) {
                opp.setEstimatedPurchaseAmountWan(opp.getEstimatedPurchaseAmount());
            }
        } else if (opp.getEstimatedPurchaseAmountWan() != null) {
            opp.setEstimatedPurchaseAmount(opp.getEstimatedPurchaseAmountWan());
            opp.setEstimatedPurchaseAmountUnit("万元");
            opp.setValue(opp.getEstimatedPurchaseAmountWan() * 10000);
        } else if (opp.getValue() == null) {
            opp.setValue(0.0);
        }
        if (opp.getWinningAmount() != null && "万元".equals(opp.getWinningAmountUnit())) {
            opp.setWinningAmountWan(opp.getWinningAmount());
        }
        if (opp.getDeliBgDeliveryAmount() != null && "万元".equals(opp.getDeliBgDeliveryAmountUnit())) {
            opp.setDeliBgDeliveryAmountWan(opp.getDeliBgDeliveryAmount());
        }
        if (opp.getCloseDate() == null || opp.getCloseDate().isBlank()) {
            opp.setCloseDate(opp.getBidDeadline() != null ? opp.getBidDeadline() : opp.getExpectedDeliveryDate());
        }
        if (opp.getOwner() == null || opp.getOwner().isBlank()) {
            opp.setOwner(opp.getSales() != null && !opp.getSales().isBlank() ? opp.getSales() : getCurrentUserName(userName));
        }
        if (opp.getSales() == null || opp.getSales().isBlank()) {
            opp.setSales(opp.getOwner());
        }
        if (opp.getSubmitter() == null || opp.getSubmitter().isBlank()) {
            opp.setSubmitter(getCurrentUserName(userName));
        }
        if (opp.getCreator() == null || opp.getCreator().isBlank()) {
            opp.setCreator(getCurrentUserName(userName));
        }
        if (creating && (opp.getSubmitDate() == null || opp.getSubmitDate().isBlank())) {
            opp.setSubmitDate(getTodayString());
        }
        opp.setLastEditor(getCurrentUserName(userName));
        opp.setLastEditDate(getNowString());
        opp.setRemindEditDate(LocalDate.now().plusDays(7).toString());
    }

    private void assignVisibility(Opportunity opp, SystemUser user, boolean creating) {
        if (user == null) {
            return;
        }
        if ("UNASSIGNED".equalsIgnoreCase(opp.getVisibilityStatus())) {
            opp.setCreatorUserId(null);
            opp.setSubmitterUserId(null);
            opp.setOwnerUserId(null);
            return;
        }
        if (creating || opp.getCreatorUserId() == null || opp.getCreatorUserId().isBlank()) {
            opp.setCreatorUserId(user.getPlatformUserId());
            opp.setCreatorName(user.getName());
        }
        if (opp.getSubmitterUserId() == null || opp.getSubmitterUserId().isBlank()) {
            opp.setSubmitterUserId(user.getPlatformUserId());
            opp.setSubmitterName(user.getName());
        }
        if (opp.getOwnerUserId() == null || opp.getOwnerUserId().isBlank()) {
            opp.setOwnerUserId(user.getPlatformUserId());
            opp.setOwnerName(user.getName());
        }
        if (opp.getCreatorName() == null || opp.getCreatorName().isBlank()) {
            opp.setCreatorName(user.getName());
        }
        if (opp.getSubmitterName() == null || opp.getSubmitterName().isBlank()) {
            opp.setSubmitterName(opp.getSubmitter() != null ? opp.getSubmitter() : user.getName());
        }
        if (opp.getOwnerName() == null || opp.getOwnerName().isBlank()) {
            opp.setOwnerName(opp.getOwner() != null ? opp.getOwner() : user.getName());
        }
        if (opp.getVisibilityStatus() == null || opp.getVisibilityStatus().isBlank()) {
            opp.setVisibilityStatus("ASSIGNED");
        }
    }

    private void replaceOpportunityFields(Opportunity opp, Opportunity updatedData, String userName) {
        opp.setName(updatedData.getName());
        opp.setCompany(updatedData.getCompany());
        opp.setStage(updatedData.getStage());
        opp.setValue(updatedData.getValue());
        opp.setProbability(updatedData.getProbability());
        opp.setCloseDate(updatedData.getCloseDate());
        opp.setContactName(updatedData.getContactName());
        opp.setContactPhone(updatedData.getContactPhone());
        opp.setContactEmail(updatedData.getContactEmail());
        opp.setSource(updatedData.getSource());
        opp.setPriority(updatedData.getPriority());
        opp.setOwner(updatedData.getOwner());
        opp.setSubmitDate(updatedData.getSubmitDate());
        opp.setSubmitter(updatedData.getSubmitter());
        opp.setGovMarketManager(updatedData.getGovMarketManager());
        opp.setSubmitterRegion(updatedData.getSubmitterRegion());
        opp.setIndustry(updatedData.getIndustry());
        opp.setSupplierCompany(updatedData.getSupplierCompany());
        opp.setDeviceTypes(updatedData.getDeviceTypes());
        opp.setDeviceModels(updatedData.getDeviceModels());
        opp.setDemandQuantity(updatedData.getDemandQuantity());
        opp.setEstimatedPurchaseAmount(updatedData.getEstimatedPurchaseAmount());
        opp.setEstimatedPurchaseAmountUnit(updatedData.getEstimatedPurchaseAmountUnit());
        opp.setEstimatedPurchaseAmountWan(updatedData.getEstimatedPurchaseAmountWan());
        opp.setCanPrepareParams(updatedData.getCanPrepareParams());
        opp.setWinRateLabel(updatedData.getWinRateLabel());
        opp.setSupplyRegion(updatedData.getSupplyRegion());
        opp.setRequiresExclusiveAuthorization(updatedData.getRequiresExclusiveAuthorization());
        opp.setAuthorizedCategories(updatedData.getAuthorizedCategories());
        opp.setExpectedDeliveryDate(updatedData.getExpectedDeliveryDate());
        opp.setBidDeadline(updatedData.getBidDeadline());
        opp.setBidWon(updatedData.getBidWon());
        opp.setPurchaseType(updatedData.getPurchaseType());
        opp.setWinningAmount(updatedData.getWinningAmount());
        opp.setWinningAmountUnit(updatedData.getWinningAmountUnit());
        opp.setWinningAmountWan(updatedData.getWinningAmountWan());
        opp.setDeliBgDeliveryAmount(updatedData.getDeliBgDeliveryAmount());
        opp.setDeliBgDeliveryAmountUnit(updatedData.getDeliBgDeliveryAmountUnit());
        opp.setDeliBgDeliveryAmountWan(updatedData.getDeliBgDeliveryAmountWan());
        opp.setSalesDepartment(updatedData.getSalesDepartment());
        opp.setReportedSuccessfully(updatedData.getReportedSuccessfully());
        opp.setSales(updatedData.getSales());
        opp.setCreator(updatedData.getCreator());
        opp.setCreatorUserId(updatedData.getCreatorUserId());
        opp.setCreatorName(updatedData.getCreatorName());
        opp.setSubmitterUserId(updatedData.getSubmitterUserId());
        opp.setSubmitterName(updatedData.getSubmitterName());
        opp.setOwnerUserId(updatedData.getOwnerUserId());
        opp.setOwnerName(updatedData.getOwnerName());
        opp.setVisibilityStatus(updatedData.getVisibilityStatus());
        opp.setProvinceGeneralManager(updatedData.getProvinceGeneralManager());
        opp.setA4BusinessManager(updatedData.getA4BusinessManager());
        opp.setA3BusinessManager(updatedData.getA3BusinessManager());
        opp.setBusinessProgressStatus(updatedData.getBusinessProgressStatus());
        opp.setReminderStatus(updatedData.getReminderStatus());
        opp.setWeeklyProgress(updatedData.getWeeklyProgress());
        opp.setLegacyExtraJson(updatedData.getLegacyExtraJson());
        opp.setDescription(updatedData.getDescription());
        normalizeOpportunity(opp, userName, false);
    }

    private OpportunityReminder buildReminder(Opportunity opp, String type, String targetUser, String content) {
        return new OpportunityReminder(type, targetUser, content, getNowString(), "已发送", getNowString(), opp);
    }

    private void attachChildren(Opportunity opp) {
        for (TaskItem task : opp.getTasks()) {
            task.setOpportunity(opp);
        }
        for (ActivityLog activity : opp.getActivities()) {
            activity.setOpportunity(opp);
        }
        for (OpportunityAttachment attachment : opp.getAttachments()) {
            attachment.setOpportunity(opp);
            if (attachment.getUploadedAt() == null || attachment.getUploadedAt().isBlank()) {
                attachment.setUploadedAt(getNowString());
            }
            if (attachment.getUploadedBy() == null || attachment.getUploadedBy().isBlank()) {
                attachment.setUploadedBy(opp.getCreator());
            }
            if (attachment.getFileUrl() == null || attachment.getFileUrl().isBlank()) {
                attachment.setFileUrl("/mock-files/" + attachment.getFileName());
            }
        }
        for (OpportunityReminder reminder : opp.getReminders()) {
            reminder.setOpportunity(opp);
        }
    }

    private void addSubmissionReminders(Opportunity opp, String userId) {
        String content = String.format("商机提报成功\n项目名称：%s\n采购单位：%s\n业务进度：%s\n请按计划持续更新项目进展。",
                opp.getName(), opp.getCompany(), opp.getBusinessProgressStatus());
        opp.getReminders().add(buildReminder(opp, "提报成功", userId, content));
        feishuService.sendAppMessage(userId, content);

        if (opp.getBidDeadline() != null && !opp.getBidDeadline().isBlank()) {
            String bidContent = String.format("投标截止提醒\n项目名称：%s\n截止时间：%s\n请及时更新投标准备和中标结果。",
                    opp.getName(), opp.getBidDeadline());
            opp.getReminders().add(buildReminder(opp, "投标截止", userId, bidContent));
        }

        if (opp.getExpectedDeliveryDate() != null && !opp.getExpectedDeliveryDate().isBlank()) {
            String deliveryContent = String.format("预计交付提醒\n项目名称：%s\n预计交付：%s\n请关注交付进度。",
                    opp.getName(), opp.getExpectedDeliveryDate());
            opp.getReminders().add(buildReminder(opp, "预计交付", userId, deliveryContent));
        }
    }

    private List<Opportunity> findDuplicateCandidates(Opportunity candidate) {
        return oppRepository.findAll().stream()
                .filter(opp -> candidate.getId() == null || !candidate.getId().equals(opp.getId()))
                .filter(opp -> {
                    boolean strong = Objects.equals(opp.getCompany(), candidate.getCompany())
                            && Objects.equals(opp.getName(), candidate.getName())
                            && Objects.equals(opp.getSupplyRegion(), candidate.getSupplyRegion());
                    boolean weak = Objects.equals(opp.getCompany(), candidate.getCompany())
                            && (Objects.equals(opp.getBidDeadline(), candidate.getBidDeadline())
                                || containsIgnoreCase(opp.getDeviceTypes(), candidate.getDeviceTypes())
                                || containsIgnoreCase(opp.getDeviceModels(), candidate.getDeviceModels()));
                    return strong || weak;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<Opportunity> findDuplicateCandidates(Opportunity candidate, SystemUser currentUser) {
        return findDuplicateCandidates(candidate).stream()
                .filter(opp -> permissionService.canView(currentUser, opp))
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<Opportunity> getAll(
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName) {
        
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        Set<String> visibleUserIds = permissionService.getVisibleUserIds(currentUser);
        
        // For Kanban default query, only active
        params.put("activeOnly", "true");
        Specification<Opportunity> spec = OpportunitySpec.filterBy(params, isAdmin, visibleUserIds);
        return oppRepository.findAll(spec);
    }

    @GetMapping("/page")
    public Page<Opportunity> getPage(
            @RequestParam Map<String, String> params,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName) {
        
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        Set<String> visibleUserIds = permissionService.getVisibleUserIds(currentUser);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<Opportunity> spec = OpportunitySpec.filterBy(params, isAdmin, visibleUserIds);
        return oppRepository.findAll(spec, pageable);
    }

    @GetMapping("/export-excel")
    public void exportExcel(
            @RequestParam Map<String, String> params,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
            
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        Set<String> visibleUserIds = permissionService.getVisibleUserIds(currentUser);
        
        Specification<Opportunity> spec = OpportunitySpec.filterBy(params, isAdmin, visibleUserIds);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = java.net.URLEncoder.encode("商机明细导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        
        try (com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), OpportunityExportDTO.class).build()) {
            com.alibaba.excel.write.metadata.WriteSheet writeSheet = EasyExcel.writerSheet("商机明细").build();
            int page = 0;
            int size = 1000;
            org.springframework.data.domain.Page<Opportunity> pageResult;
            do {
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
                pageResult = oppRepository.findAll(spec, pageable);
                
                List<OpportunityExportDTO> exportList = pageResult.getContent().stream().map(opp -> {
                    OpportunityExportDTO dto = new OpportunityExportDTO();
                    dto.setName(opp.getName());
                    dto.setCompany(opp.getCompany());
                    dto.setStage(STAGES.getOrDefault(opp.getStage(), opp.getStage()));
                    dto.setValue(opp.getValue());
                    dto.setProbability(opp.getProbability());
                    dto.setCloseDate(opp.getCloseDate());
                    dto.setOwner(opp.getOwner());
                    dto.setPriority(opp.getPriority());
                    dto.setSource(opp.getSource());
                    dto.setContactName(opp.getContactName());
                    dto.setContactPhone(opp.getContactPhone());
                    dto.setIndustry(opp.getIndustry());
                    dto.setSubmitter(opp.getSubmitter());
                    dto.setSubmitDate(opp.getSubmitDate());
                    dto.setSupplyRegion(opp.getSupplyRegion());
                    dto.setBusinessProgressStatus(opp.getBusinessProgressStatus());
                    dto.setBidWonStr(opp.getBidWon() != null ? (opp.getBidWon() ? "是" : "否") : "未知");
                    dto.setDescription(opp.getDescription());
                    return dto;
                }).collect(Collectors.toList());
                
                excelWriter.write(exportList, writeSheet);
                page++;
            } while (pageResult.hasNext());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Opportunity> getById(@PathVariable Long id,
                                               @RequestParam(required = false) String userId,
                                               @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id)
                .filter(opp -> permissionService.canView(currentUser, opp))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @PostMapping
    public Opportunity create(@RequestBody Opportunity opp,
                              @RequestParam(required = false) String userId,
                              @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        if (!currentUser.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户已禁用，不能提报商机");
        }
        boolean adminUnassignedImport = currentUser.isAdmin() && "UNASSIGNED".equalsIgnoreCase(opp.getVisibilityStatus());
        if (!adminUnassignedImport) {
            opp.setVisibilityStatus("ASSIGNED");
        }
        normalizeOpportunity(opp, userName, true);
        assignVisibility(opp, currentUser, true);
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(new TaskItem("完善设备需求和授权信息", false, opp));
        tasks.add(new TaskItem("按提醒周期更新项目进展", false, opp));
        opp.setTasks(tasks);

        List<ActivityLog> activities = new ArrayList<>();
        String stageLabel = STAGES.getOrDefault(opp.getStage(), opp.getStage());
        activities.add(new ActivityLog("system", "成功录入商机，初始阶段：[" + stageLabel + "]，业务进度：[" + opp.getBusinessProgressStatus() + "]", getNowString(), opp));
        opp.setActivities(activities);

        addSubmissionReminders(opp, getCurrentUserId(userId));
        attachChildren(opp);
        return oppRepository.save(opp);
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @PostMapping("/submissions")
    public Opportunity submit(@RequestBody Opportunity opp,
                              @RequestParam(required = false) String userId,
                              @RequestParam(required = false) String userName) {
        return create(opp, userId, userName);
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @PutMapping("/{id}")
    public ResponseEntity<Opportunity> update(@PathVariable Long id,
                                              @RequestBody Opportunity updatedData,
                                              @RequestParam(required = false) String userId,
                                              @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canEdit(currentUser, opp)) {
                return ResponseEntity.status(403).<Opportunity>build();
            }
            boolean lockedDeviceTypeChange = isDeviceTypeLocked(opp) && isDeviceTypeChanged(opp, updatedData);
            if (lockedDeviceTypeChange && !currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).<Opportunity>build();
            }
            Map<String, String> before = auditService.snapshot(opp);
            String oldStage = opp.getStage();
            String oldProgress = opp.getBusinessProgressStatus();
            String permissionSource = permissionService.permissionSource(currentUser, opp);
            boolean adminDeviceTypeOverride = lockedDeviceTypeChange && currentUser.isAdmin();
            String creatorUserId = opp.getCreatorUserId();
            String creatorName = opp.getCreatorName();
            String submitterUserId = opp.getSubmitterUserId();
            String submitterName = opp.getSubmitterName();
            String ownerUserId = opp.getOwnerUserId();
            String ownerName = opp.getOwnerName();
            String visibilityStatus = opp.getVisibilityStatus();
            replaceOpportunityFields(opp, updatedData, userName);
            opp.setCreatorUserId(creatorUserId);
            opp.setCreatorName(creatorName);
            opp.setSubmitterUserId(submitterUserId);
            opp.setSubmitterName(submitterName);
            opp.setOwnerUserId(ownerUserId);
            opp.setOwnerName(ownerName);
            opp.setVisibilityStatus(visibilityStatus);

            if (hasDeviceRequirementVersionChange(before, opp)) {
                opp.setDeviceRequirementVersion(nextDeviceRequirementVersion(opp));
            }

            if (adminDeviceTypeOverride) {
                ActivityLog overrideLog = new ActivityLog("system",
                        "管理员特殊修正已锁定的需求设备类型，已记录高风险操作。",
                        getNowString(), opp);
                opp.getActivities().add(0, overrideLog);
            }

            if (!Objects.equals(oldStage, opp.getStage())) {
                String oldLabel = STAGES.getOrDefault(oldStage, oldStage);
                String newLabel = STAGES.getOrDefault(opp.getStage(), opp.getStage());
                ActivityLog log = new ActivityLog("system",
                        "商机阶段由 [" + oldLabel + "] 变更为 [" + newLabel + "]，赢率更新为 " + opp.getProbability() + "%",
                        getNowString(), opp);
                opp.getActivities().add(0, log);
            } else if (!Objects.equals(oldProgress, opp.getBusinessProgressStatus())) {
                ActivityLog log = new ActivityLog("system",
                        "业务进度由 [" + oldProgress + "] 变更为 [" + opp.getBusinessProgressStatus() + "]",
                        getNowString(), opp);
                opp.getActivities().add(0, log);
            } else {
                ActivityLog log = new ActivityLog("system", "更新修改商机提报资料", getNowString(), opp);
                opp.getActivities().add(0, log);
            }

            Opportunity saved = oppRepository.save(opp);
            auditService.recordChanges(saved, before, currentUser, "PC", permissionSource);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @PutMapping("/submissions/{id}")
    public ResponseEntity<Opportunity> updateSubmission(@PathVariable Long id,
                                                        @RequestBody Opportunity updatedData,
                                                        @RequestParam(required = false) String userId,
                                                        @RequestParam(required = false) String userName) {
        return update(id, updatedData, userId, userName);
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestParam(required = false) String userId,
                                       @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        if (!currentUser.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return oppRepository.findById(id).map(opp -> {
            oppRepository.delete(opp);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/submissions/check-duplicates")
    public Map<String, Object> checkDuplicates(@RequestBody Opportunity candidate,
                                               @RequestParam(required = false) String userId,
                                               @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        Map<String, Object> result = new HashMap<>();
        List<Opportunity> matches = findDuplicateCandidates(candidate, currentUser);
        result.put("duplicate", !matches.isEmpty());
        result.put("matches", matches);
        return result;
    }

    @GetMapping("/mine/page")
    public Page<Opportunity> getMinePage(
            @RequestParam Map<String, String> params,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        Set<String> visibleUserIds = permissionService.getVisibleUserIds(currentUser);
        params.put("activeOnly", "true");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<Opportunity> spec = OpportunitySpec.filterBy(params, isAdmin, visibleUserIds);
        return oppRepository.findAll(spec, pageable);
    }

    @GetMapping("/mine")
    public List<Opportunity> getMine(@RequestParam Map<String, String> params,
                                     @RequestParam(required = false) String userId,
                                     @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        Set<String> visibleUserIds = permissionService.getVisibleUserIds(currentUser);
        params.put("activeOnly", "true");
        // Keep it simple for Kanban
        Specification<Opportunity> spec = OpportunitySpec.filterBy(params, isAdmin, visibleUserIds);
        return oppRepository.findAll(spec);
    }

    @GetMapping("/{id}/change-logs")
    public ResponseEntity<List<OpportunityChangeLog>> getChangeLogs(@PathVariable Long id,
                                                                    @RequestParam(required = false) String userId,
                                                                    @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id)
                .filter(opp -> permissionService.canView(currentUser, opp))
                .map(opp -> ResponseEntity.ok(auditService.listLogs(id)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/options")
    public Map<String, Object> getOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("stages", STAGES);
        options.put("industries", List.of("政府", "金融", "医疗", "教育", "能源/矿产", "交通/物流", "军队", "制造", "电信", "酒店文旅/连锁商业", "军工", "其他"));
        options.put("deviceTypes", List.of("数码打印A4", "数码打印A3", "办公设备", "办公纸品", "打印耗材"));
        options.put("authorizedCategories", List.of("A3复印机", "A4激光打印机", "A4激光多功能一体机", "A4喷墨多功能一体机", "耗材", "配件"));
        options.put("winRates", new ArrayList<>(WIN_RATE_VALUES.keySet()));
        options.put("purchaseTypes", List.of("一次性采购（一次性交付）", "带量采购（分批交付）", "框采入围", "电商平台线上采购"));
        options.put("businessProgressStatuses", List.of("新提报", "参数准备", "授权处理中", "已报备", "投标准备", "已投标", "中标", "未中标", "交付中", "已交付", "暂停/搁置"));
        options.put("amountUnits", List.of("万元", "元"));
        options.put("regions", List.of("京津蒙", "闽赣", "黑吉", "云贵", "川渝", "粤桂", "辽宁", "江苏", "河南", "山东", "浙江", "总部项目"));
        return options;
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @PostMapping("/{id}/attachments")
    public ResponseEntity<OpportunityAttachment> addAttachment(@PathVariable Long id,
                                                               @RequestBody OpportunityAttachment body,
                                                               @RequestParam(required = false) String userId,
                                                               @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canEdit(currentUser, opp)) {
                return ResponseEntity.status(403).<OpportunityAttachment>build();
            }
            OpportunityAttachment attachment = new OpportunityAttachment(
                    body.getFileName(),
                    body.getFileUrl() == null || body.getFileUrl().isBlank() ? "/mock-files/" + body.getFileName() : body.getFileUrl(),
                    body.getFileType(),
                    getCurrentUserName(userName),
                    getNowString(),
                    opp);
            opp.getAttachments().add(attachment);
            opp.getActivities().add(0, new ActivityLog("system", "上传附件：" + attachment.getFileName(), getNowString(), opp));
            oppRepository.save(opp);
            return ResponseEntity.ok(attachment);
        }).orElse(ResponseEntity.notFound().build());
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id,
                                                @PathVariable Long attachmentId,
                                                @RequestParam(required = false) String userId,
                                                @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canEdit(currentUser, opp)) {
                return ResponseEntity.status(403).<Void>build();
            }
            opp.getAttachments().removeIf(a -> a.getId().equals(attachmentId));
            opp.getActivities().add(0, new ActivityLog("system", "删除附件：" + attachmentId, getNowString(), opp));
            oppRepository.save(opp);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @CacheEvict(value = {"dashboard_metrics", "dashboard_funnel", "dashboard_stages", "dashboard_trend"}, allEntries = true)
    @PostMapping("/{id}/reminders")
    public ResponseEntity<OpportunityReminder> addReminder(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body,
                                                           @RequestParam(required = false) String userId,
                                                           @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canView(currentUser, opp)) {
                return ResponseEntity.status(403).<OpportunityReminder>build();
            }
            String target = body.getOrDefault("targetUser", getCurrentUserId(userId));
            String content = body.getOrDefault("content", "请及时跟进商机：" + opp.getName());
            OpportunityReminder reminder = buildReminder(opp, body.getOrDefault("reminderType", "手动提醒"), target, content);
            opp.getReminders().add(reminder);
            feishuService.sendAppMessage(target, content);
            oppRepository.save(opp);
            return ResponseEntity.ok(reminder);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reminders/config")
    public ResponseEntity<Map<String, String>> getReminderConfig(@RequestParam(required = false) String userId,
                                                                 @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        if (currentUser == null || !(currentUser.isAdmin() || currentUser.getCanViewAll())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(reminderService.getReminderConfigs());
    }

    @PostMapping("/reminders/config")
    public ResponseEntity<Map<String, Object>> saveReminderConfig(@RequestBody Map<String, String> configs,
                                                                  @RequestParam(required = false) String userId,
                                                                  @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        if (currentUser == null || !(currentUser.isAdmin() || currentUser.getCanViewAll())) {
            Map<String, Object> forbidden = new HashMap<>();
            forbidden.put("success", false);
            forbidden.put("message", "只有管理员才能修改提醒配置");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(forbidden);
        }
        reminderService.saveReminderConfigs(configs);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "提醒配置保存成功！");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reminders/trigger-scan")
    public ResponseEntity<Map<String, Object>> triggerReminderScan(@RequestParam(required = false) String userId,
                                                                   @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        if (currentUser == null || !(currentUser.isAdmin() || currentUser.getCanViewAll())) {
            Map<String, Object> forbidden = new HashMap<>();
            forbidden.put("success", false);
            forbidden.put("message", "只有管理员才能手动触发提醒扫描");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(forbidden);
        }
        int sentCount = reminderService.runReminderScan();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", String.format("提醒扫描执行完成，共发出 %d 条消息通知！", sentCount));
        result.put("sentCount", sentCount);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics(@RequestParam(required = false) String userId,
                                          @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        System.out.println("[OpportunityController.getMetrics] userId=" + currentUser.getPlatformUserId() + ", role=" + currentUser.getRole() + ", isAdmin=" + isAdmin);
        List<String> visibleUserIds = new ArrayList<>(permissionService.getVisibleUserIds(currentUser));
        if (visibleUserIds.isEmpty() && !isAdmin) visibleUserIds.add("-1"); // Prevent empty IN clause

        Double totalValue = oppRepository.sumValueByPermissions(isAdmin, visibleUserIds);
        Long activeCount = oppRepository.countActiveByPermissions(isAdmin, visibleUserIds);
        
        List<Object[]> stageCounts = oppRepository.getStageCountsByPermissions(isAdmin, visibleUserIds);
        long won = 0;
        long lost = 0;
        for (Object[] row : stageCounts) {
            String stage = (String) row[0];
            Long count = (Long) row[1];
            if ("won".equals(stage)) won += count;
            if ("lost".equals(stage)) lost += count;
        }

        double winRate = (won + lost) > 0 ? ((double) won / (won + lost) * 100) : 0.0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalPipeline", totalValue != null ? totalValue : 0.0);
        metrics.put("activeCount", activeCount != null ? activeCount : 0L);
        metrics.put("winRate", winRate);
        metrics.put("avgValue", 0.0); // Simplified to 0.0 for dashboard performance
        metrics.put("reportedCount", 0L); // Deprecated in dashboard
        metrics.put("authorizationCount", 0L); // Deprecated in dashboard
        System.out.println("[OpportunityController.getMetrics] result=" + metrics);
        return metrics;
    }

    @GetMapping("/charts/funnel")
    public Map<String, Double> getFunnelChart(@RequestParam(required = false) String userId,
                                              @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        System.out.println("[OpportunityController.getFunnelChart] userId=" + currentUser.getPlatformUserId() + ", role=" + currentUser.getRole() + ", isAdmin=" + isAdmin);
        List<String> visibleUserIds = new ArrayList<>(permissionService.getVisibleUserIds(currentUser));
        if (visibleUserIds.isEmpty() && !isAdmin) visibleUserIds.add("-1");

        Map<String, Double> funnel = initFunnelMap();
        List<Object[]> funnelMetrics = oppRepository.getFunnelMetricsByPermissions(isAdmin, visibleUserIds);
        for (Object[] row : funnelMetrics) {
            String stage = (String) row[0];
            Double value = (Double) row[1];
            if (stage != null && funnel.containsKey(stage)) {
                funnel.put(stage, value != null ? value : 0.0);
            }
        }
        System.out.println("[OpportunityController.getFunnelChart] result=" + funnel);
        return funnel;
    }

    @GetMapping("/charts/stages")
    public Map<String, Long> getStagesChart(@RequestParam(required = false) String userId,
                                            @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        System.out.println("[OpportunityController.getStagesChart] userId=" + currentUser.getPlatformUserId() + ", role=" + currentUser.getRole() + ", isAdmin=" + isAdmin);
        List<String> visibleUserIds = new ArrayList<>(permissionService.getVisibleUserIds(currentUser));
        if (visibleUserIds.isEmpty() && !isAdmin) visibleUserIds.add("-1");

        Map<String, Long> counts = initStageCountMap();
        List<Object[]> stageCounts = oppRepository.getStageCountsByPermissions(isAdmin, visibleUserIds);
        for (Object[] row : stageCounts) {
            String stage = (String) row[0];
            Long count = (Long) row[1];
            if (stage != null && counts.containsKey(stage)) {
                counts.put(stage, count != null ? count : 0L);
            }
        }
        System.out.println("[OpportunityController.getStagesChart] result=" + counts);
        return counts;
    }

    @GetMapping("/charts/trend")
    public Map<String, Double> getTrendChart(@RequestParam(required = false) String userId,
                                             @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        boolean isAdmin = currentUser.isAdmin();
        System.out.println("[OpportunityController.getTrendChart] userId=" + currentUser.getPlatformUserId() + ", role=" + currentUser.getRole() + ", isAdmin=" + isAdmin);
        List<String> visibleUserIds = new ArrayList<>(permissionService.getVisibleUserIds(currentUser));
        if (visibleUserIds.isEmpty() && !isAdmin) visibleUserIds.add("-1");

        Map<String, Double> trend = new TreeMap<>();
        List<Object[]> trendMetrics = oppRepository.getTrendByPermissions(isAdmin, visibleUserIds);
        for (Object[] row : trendMetrics) {
            String month = (String) row[0];
            Double value = (Double) row[1];
            if (month != null) {
                trend.put(month, value != null ? value : 0.0);
            }
        }
        System.out.println("[OpportunityController.getTrendChart] result=" + trend);
        return trend;
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskItem> addTask(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            @RequestParam(required = false) String userId,
                                            @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canEdit(currentUser, opp)) {
                return ResponseEntity.status(403).<TaskItem>build();
            }
            TaskItem task = new TaskItem(body.get("text"), false, opp);
            opp.getTasks().add(task);
            opp.getActivities().add(0, new ActivityLog("system", "新建待办任务：\"" + task.getText() + "\"", getNowString(), opp));
            oppRepository.save(opp);
            return ResponseEntity.ok(task);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/tasks/{taskId}/toggle")
    public ResponseEntity<TaskItem> toggleTask(@PathVariable Long id,
                                               @PathVariable Long taskId,
                                               @RequestParam(required = false) String userId,
                                               @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canEdit(currentUser, opp)) {
                return ResponseEntity.status(403).<TaskItem>build();
            }
            for (TaskItem t : opp.getTasks()) {
                if (t.getId().equals(taskId)) {
                    boolean nextStatus = !t.isDone();
                    t.setDone(nextStatus);
                    String textLog = nextStatus ? "完成了待办任务" : "重新开启待办任务";
                    opp.getActivities().add(0, new ActivityLog("system", textLog + "：\"" + t.getText() + "\"", getNowString(), opp));
                    oppRepository.save(opp);
                    return ResponseEntity.ok(t);
                }
            }
            return ResponseEntity.notFound().<TaskItem>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityLog> addActivity(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body,
                                                   @RequestParam(required = false) String userId,
                                                   @RequestParam(required = false) String userName) {
        SystemUser currentUser = resolveCurrentUser(userId, userName);
        return oppRepository.findById(id).map(opp -> {
            if (!permissionService.canEdit(currentUser, opp)) {
                return ResponseEntity.status(403).<ActivityLog>build();
            }
            ActivityLog log = new ActivityLog(body.get("type"), body.get("content"), getNowString(), opp);
            opp.getActivities().add(0, log);
            opp.setWeeklyProgress(body.getOrDefault("content", opp.getWeeklyProgress()));
            opp.setLastEditDate(getNowString());
            opp.setRemindEditDate(LocalDate.now().plusDays(7).toString());
            oppRepository.save(opp);
            return ResponseEntity.ok(log);
        }).orElse(ResponseEntity.notFound().build());
    }
}
