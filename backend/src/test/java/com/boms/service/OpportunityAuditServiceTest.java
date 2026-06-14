package com.boms.service;

import com.boms.model.Opportunity;
import com.boms.model.SystemUser;
import com.boms.repository.OpportunityChangeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OpportunityAuditServiceTest {

    @Mock
    private OpportunityChangeLogRepository changeLogRepository;

    @InjectMocks
    private OpportunityAuditService auditService;

    private Opportunity opportunity;
    private SystemUser editor;

    @BeforeEach
    void setUp() {
        opportunity = new Opportunity();
        opportunity.setId(1L);
        opportunity.setName("Test Opportunity");
        opportunity.setStage("negotiation");

        editor = new SystemUser();
        editor.setWecomUserId("editor123");
        editor.setName("Test Editor");
    }

    @Test
    void testSnapshot() {
        Map<String, String> snapshot = auditService.snapshot(opportunity);
        assertNotNull(snapshot);
        assertEquals("Test Opportunity", snapshot.get("name"));
        assertEquals("negotiation", snapshot.get("stage"));
    }

    @Test
    void testRecordChanges_WithModifications() {
        Map<String, String> beforeSnapshot = auditService.snapshot(opportunity);

        // Modify opportunity
        opportunity.setName("Updated Opportunity");
        opportunity.setStage("won");

        auditService.recordChanges(opportunity, beforeSnapshot, editor, "API", "Self");

        // Verify that 2 change logs were saved (name and stage)
        verify(changeLogRepository, times(2)).save(any());
    }

    @Test
    void testRecordChanges_NoModifications() {
        Map<String, String> beforeSnapshot = auditService.snapshot(opportunity);

        auditService.recordChanges(opportunity, beforeSnapshot, editor, "API", "Self");

        // Verify that no change logs were saved
        verify(changeLogRepository, times(0)).save(any());
    }
}
