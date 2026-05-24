package com.boms.service;

import com.boms.model.Opportunity;
import com.boms.model.OpportunityChangeLog;
import com.boms.model.SystemUser;
import com.boms.repository.OpportunityChangeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class OpportunityAuditService {
    @Autowired
    private OpportunityChangeLogRepository changeLogRepository;

    private static final Map<String, FieldSnapshot> AUDITED_FIELDS = new LinkedHashMap<>();

    static {
        AUDITED_FIELDS.put("name", new FieldSnapshot("项目名称", Opportunity::getName));
        AUDITED_FIELDS.put("company", new FieldSnapshot("采购单位", Opportunity::getCompany));
        AUDITED_FIELDS.put("stage", new FieldSnapshot("商机阶段", Opportunity::getStage));
        AUDITED_FIELDS.put("winRateLabel", new FieldSnapshot("赢率", Opportunity::getWinRateLabel));
        AUDITED_FIELDS.put("businessProgressStatus", new FieldSnapshot("业务进度", Opportunity::getBusinessProgressStatus));
        AUDITED_FIELDS.put("estimatedPurchaseAmount", new FieldSnapshot("预估采购金额", opp -> stringify(opp.getEstimatedPurchaseAmount())));
        AUDITED_FIELDS.put("estimatedPurchaseAmountUnit", new FieldSnapshot("预估金额单位", Opportunity::getEstimatedPurchaseAmountUnit));
        AUDITED_FIELDS.put("bidDeadline", new FieldSnapshot("投标截止时间", Opportunity::getBidDeadline));
        AUDITED_FIELDS.put("expectedDeliveryDate", new FieldSnapshot("预计交付时间", Opportunity::getExpectedDeliveryDate));
        AUDITED_FIELDS.put("bidWon", new FieldSnapshot("是否中标", opp -> stringify(opp.getBidWon())));
        AUDITED_FIELDS.put("reportedSuccessfully", new FieldSnapshot("是否报备成功", opp -> stringify(opp.getReportedSuccessfully())));
        AUDITED_FIELDS.put("sales", new FieldSnapshot("销售", Opportunity::getSales));
        AUDITED_FIELDS.put("owner", new FieldSnapshot("负责人", Opportunity::getOwner));
        AUDITED_FIELDS.put("weeklyProgress", new FieldSnapshot("本周项目进展", Opportunity::getWeeklyProgress));
    }

    public Map<String, String> snapshot(Opportunity opp) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, FieldSnapshot> entry : AUDITED_FIELDS.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().extractor.apply(opp));
        }
        return snapshot;
    }

    public void recordChanges(Opportunity opp, Map<String, String> before, SystemUser editor, String source, String permissionSource) {
        Map<String, String> after = snapshot(opp);
        for (Map.Entry<String, String> entry : after.entrySet()) {
            String oldValue = before.get(entry.getKey());
            String newValue = entry.getValue();
            if (!Objects.equals(oldValue, newValue)) {
                FieldSnapshot field = AUDITED_FIELDS.get(entry.getKey());
                OpportunityChangeLog log = new OpportunityChangeLog(
                        opp,
                        editor != null ? editor.getWecomUserId() : null,
                        editor != null ? editor.getName() : null,
                        now(),
                        entry.getKey(),
                        field.label,
                        oldValue,
                        newValue,
                        source,
                        permissionSource,
                        opp.getOwnerUserId()
                );
                changeLogRepository.save(log);
            }
        }
    }

    public List<OpportunityChangeLog> listLogs(Long opportunityId) {
        return changeLogRepository.findByOpportunity_IdOrderByEditedAtDesc(opportunityId);
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static class FieldSnapshot {
        private final String label;
        private final Function<Opportunity, String> extractor;

        private FieldSnapshot(String label, Function<Opportunity, String> extractor) {
            this.label = label;
            this.extractor = extractor;
        }
    }
}
