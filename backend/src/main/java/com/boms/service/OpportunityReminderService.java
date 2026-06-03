package com.boms.service;

import com.boms.model.Opportunity;
import com.boms.model.OpportunityReminder;
import com.boms.model.SystemConfig;
import com.boms.repository.OpportunityRepository;
import com.boms.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpportunityReminderService {

    private static final Logger logger = LoggerFactory.getLogger(OpportunityReminderService.class);

    @Autowired
    private OpportunityRepository oppRepository;

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private WeComService weComService;

    @Value("${wecom.redirect-uri:http://localhost:5173/login-callback}")
    private String redirectUri;

    /**
     * Get system configuration by key, return default if not found
     */
    public String getConfig(String key, String defaultValue) {
        return configRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Save configuration key-value pair
     */
    public void saveConfig(String key, String value) {
        configRepository.save(new SystemConfig(key, value));
    }

    /**
     * Get all reminder configurations as a Map
     */
    public Map<String, String> getReminderConfigs() {
        Map<String, String> configs = new HashMap<>();
        configs.put("reminder.bid_deadline_days", getConfig("reminder.bid_deadline_days", "3"));
        configs.put("reminder.expected_delivery_days", getConfig("reminder.expected_delivery_days", "3"));
        configs.put("reminder.weekly_update_days", getConfig("reminder.weekly_update_days", "7"));

        configs.put("reminder.enable.bid_deadline", getConfig("reminder.enable.bid_deadline", "true"));
        configs.put("reminder.enable.expected_delivery", getConfig("reminder.enable.expected_delivery", "true"));
        configs.put("reminder.enable.weekly_update", getConfig("reminder.enable.weekly_update", "true"));
        configs.put("reminder.enable.auth_followup", getConfig("reminder.enable.auth_followup", "true"));
        configs.put("reminder.enable.report_followup", getConfig("reminder.enable.report_followup", "true"));
        configs.put("reminder.enable.bid_result", getConfig("reminder.enable.bid_result", "true"));
        return configs;
    }

    /**
     * Save multiple configurations at once
     */
    public void saveReminderConfigs(Map<String, String> configs) {
        if (configs != null) {
            configs.forEach(this::saveConfig);
        }
    }

    /**
     * Generate the base URL origin from the redirectUri config
     */
    private String getBaseUrl() {
        if (redirectUri == null) {
            return "http://localhost:5173";
        }
        int callbackIndex = redirectUri.indexOf("/login-callback");
        if (callbackIndex != -1) {
            return redirectUri.substring(0, callbackIndex);
        }
        return redirectUri;
    }

    private String getDetailUrl(Long oppId) {
        return getBaseUrl() + "/list?id=" + oppId;
    }

    /**
     * Execute periodic background reminder scan (Cron: daily at 9:00 AM)
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduledReminderScan() {
        logger.info("开始执行定时商机进度提醒扫描任务...");
        int sentCount = runReminderScan();
        logger.info("定时扫描完成，已发送 {} 条提醒消息。", sentCount);
    }

    /**
     * Run the reminder evaluation and send message alerts to users
     * @return count of sent reminder messages
     */
    public int runReminderScan() {
        logger.info("开始扫描商机提醒规则评估...");
        List<Opportunity> allOpps = oppRepository.findAll();
        int sentCount = 0;

        // Load configuration values
        boolean enableBidDeadline = "true".equalsIgnoreCase(getConfig("reminder.enable.bid_deadline", "true"));
        boolean enableExpectedDelivery = "true".equalsIgnoreCase(getConfig("reminder.enable.expected_delivery", "true"));
        boolean enableWeeklyUpdate = "true".equalsIgnoreCase(getConfig("reminder.enable.weekly_update", "true"));
        boolean enableAuthFollowup = "true".equalsIgnoreCase(getConfig("reminder.enable.auth_followup", "true"));
        boolean enableReportFollowup = "true".equalsIgnoreCase(getConfig("reminder.enable.report_followup", "true"));
        boolean enableBidResult = "true".equalsIgnoreCase(getConfig("reminder.enable.bid_result", "true"));

        int bidDeadlineDays = Integer.parseInt(getConfig("reminder.bid_deadline_days", "3"));
        int expectedDeliveryDays = Integer.parseInt(getConfig("reminder.expected_delivery_days", "3"));
        int weeklyUpdateDays = Integer.parseInt(getConfig("reminder.weekly_update_days", "7"));

        LocalDate today = LocalDate.now();

        for (Opportunity opp : allOpps) {
            // Find targets
            String targetUser = opp.getOwnerUserId();
            if (targetUser == null || targetUser.isBlank()) {
                targetUser = opp.getSubmitterUserId();
            }
            if (targetUser == null || targetUser.isBlank()) {
                targetUser = opp.getCreatorUserId();
            }
            if (targetUser == null || targetUser.isBlank()) {
                targetUser = "zhang_jingli"; // Sandbox default user
            }

            // 1. 投标截止提醒
            if (enableBidDeadline && opp.getBidDeadline() != null && !opp.getBidDeadline().isBlank()) {
                try {
                    LocalDate deadline = LocalDate.parse(opp.getBidDeadline());
                    long diff = ChronoUnit.DAYS.between(today, deadline);
                    if (diff == bidDeadlineDays) {
                        boolean alreadySent = opp.getReminders().stream()
                                .anyMatch(r -> "投标截止".equals(r.getReminderType()));
                        if (!alreadySent) {
                            String content = String.format("【投标截止提醒】\n项目名称：%s\n采购单位：%s\n当前阶段：%s\n待处理事项：项目投标即将截止，请及时进行投标准备或填写中标结果！\n截止日期：%s\n详情链接：%s",
                                    opp.getName(), opp.getCompany(), opp.getStage(), opp.getBidDeadline(), getDetailUrl(opp.getId()));
                            sendReminderMessage(opp, "投标截止", targetUser, content);
                            sentCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("商机 {} (ID: {}) 投标截止日期解析错误: {}", opp.getName(), opp.getId(), opp.getBidDeadline());
                }
            }

            // 2. 预计交付提醒
            if (enableExpectedDelivery && opp.getExpectedDeliveryDate() != null && !opp.getExpectedDeliveryDate().isBlank()) {
                try {
                    LocalDate delivery = LocalDate.parse(opp.getExpectedDeliveryDate());
                    long diff = ChronoUnit.DAYS.between(today, delivery);
                    if (diff == expectedDeliveryDays) {
                        boolean alreadySent = opp.getReminders().stream()
                                .anyMatch(r -> "预计交付".equals(r.getReminderType()));
                        if (!alreadySent) {
                            String content = String.format("【预计交付提醒】\n项目名称：%s\n采购单位：%s\n当前阶段：%s\n待处理事项：项目预计交付时间临近，请跟进交付进度并更新状态。\n交付日期：%s\n详情链接：%s",
                                    opp.getName(), opp.getCompany(), opp.getStage(), opp.getExpectedDeliveryDate(), getDetailUrl(opp.getId()));
                            sendReminderMessage(opp, "预计交付", targetUser, content);
                            sentCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("商机 {} (ID: {}) 预计交付日期解析错误: {}", opp.getName(), opp.getId(), opp.getExpectedDeliveryDate());
                }
            }

            // 3. 周更新提醒
            if (enableWeeklyUpdate && !"won".equalsIgnoreCase(opp.getStage()) && !"lost".equalsIgnoreCase(opp.getStage()) && opp.getBidWon() == null) {
                try {
                    String lastEditStr = opp.getLastEditDate();
                    if (lastEditStr != null && lastEditStr.length() >= 10) {
                        LocalDate lastEdit = LocalDate.parse(lastEditStr.substring(0, 10));
                        long diff = ChronoUnit.DAYS.between(lastEdit, today);
                        if (diff >= weeklyUpdateDays) {
                            // Check if sent in the last update days interval
                            boolean recentlySent = opp.getReminders().stream()
                                    .filter(r -> "周更新".equals(r.getReminderType()))
                                    .anyMatch(r -> {
                                        try {
                                            LocalDate sentDate = LocalDate.parse(r.getSentAt().substring(0, 10));
                                            return ChronoUnit.DAYS.between(sentDate, today) < weeklyUpdateDays;
                                        } catch (Exception ex) {
                                            return false;
                                        }
                                    });
                            if (!recentlySent) {
                                String content = String.format("【周更新提醒】\n项目名称：%s\n采购单位：%s\n当前阶段：%s\n待处理事项：该商机已超过 %d 天没有更新，请及时录入本周最新项目进展与备注！\n最后更新时间：%s\n详情链接：%s",
                                        opp.getName(), opp.getCompany(), opp.getStage(), diff, lastEditStr, getDetailUrl(opp.getId()));
                                sendReminderMessage(opp, "周更新", targetUser, content);
                                sentCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("商机 {} (ID: {}) 最后编辑日期解析错误: {}", opp.getName(), opp.getId(), opp.getLastEditDate());
                }
            }

            // 4. 授权跟进提醒
            if (enableAuthFollowup && Boolean.TRUE.equals(opp.getRequiresExclusiveAuthorization())) {
                boolean hasAuthCategory = opp.getAuthorizedCategories() != null && !opp.getAuthorizedCategories().isBlank();
                boolean hasAuthAttachment = opp.getAttachments() != null && opp.getAttachments().stream()
                        .anyMatch(a -> "exclusive_authorization".equalsIgnoreCase(a.getFileType()) || "authorization".equalsIgnoreCase(a.getFileType()));
                if (!hasAuthCategory || !hasAuthAttachment) {
                    boolean alreadySent = opp.getReminders().stream()
                            .anyMatch(r -> "授权跟进".equals(r.getReminderType()));
                    if (!alreadySent) {
                        String content = String.format("【授权跟进提醒】\n项目名称：%s\n采购单位：%s\n待处理事项：项目需要唯一授权，但尚未填写授权品类或上传授权证明文件，请及时处理！\n详情链接：%s",
                                opp.getName(), opp.getCompany(), getDetailUrl(opp.getId()));
                        sendReminderMessage(opp, "授权跟进", targetUser, content);
                        sentCount++;
                    }
                }
            }

            // 5. 报备提醒
            if (enableReportFollowup && !Boolean.TRUE.equals(opp.getReportedSuccessfully())) {
                if (opp.getBidDeadline() != null && !opp.getBidDeadline().isBlank()) {
                    try {
                        LocalDate deadline = LocalDate.parse(opp.getBidDeadline());
                        long diff = ChronoUnit.DAYS.between(today, deadline);
                        if (diff >= 0 && diff <= 3) {
                            boolean alreadySent = opp.getReminders().stream()
                                    .anyMatch(r -> "报备提醒".equals(r.getReminderType()));
                            if (!alreadySent) {
                                String content = String.format("【商机报备提醒】\n项目名称：%s\n采购单位：%s\n待处理事项：项目投标即将截止，但商机尚未完成成功报备，请及时跟进报备流程！\n投标截止时间：%s\n详情链接：%s",
                                        opp.getName(), opp.getCompany(), opp.getBidDeadline(), getDetailUrl(opp.getId()));
                                sendReminderMessage(opp, "报备提醒", targetUser, content);
                                sentCount++;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }

            // 6. 中标结果提醒
            if (enableBidResult && opp.getBidWon() == null && opp.getBidDeadline() != null && !opp.getBidDeadline().isBlank()) {
                try {
                    LocalDate deadline = LocalDate.parse(opp.getBidDeadline());
                    if (today.isAfter(deadline)) {
                        boolean alreadySent = opp.getReminders().stream()
                                .anyMatch(r -> "中标结果".equals(r.getReminderType()));
                        if (!alreadySent) {
                            String content = String.format("【中标结果提醒】\n项目名称：%s\n采购单位：%s\n待处理事项：项目投标截止日期已过，但尚未填写中标结果，请及时更新项目是否中标状态！\n截止日期：%s\n详情链接：%s",
                                    opp.getName(), opp.getCompany(), opp.getBidDeadline(), getDetailUrl(opp.getId()));
                            sendReminderMessage(opp, "中标结果", targetUser, content);
                            sentCount++;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return sentCount;
    }

    /**
     * Send direct WeCom message and save reminder record in opportunity
     */
    private void sendReminderMessage(Opportunity opp, String type, String targetUser, String content) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        OpportunityReminder reminder = new OpportunityReminder(type, targetUser, content, now, "已发送", now, opp);
        opp.getReminders().add(reminder);
        weComService.sendAppMessage(targetUser, content);
        oppRepository.save(opp);
    }
}
