package com.boms.repository;

import com.boms.model.OpportunityChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpportunityChangeLogRepository extends JpaRepository<OpportunityChangeLog, Long> {
    List<OpportunityChangeLog> findByOpportunity_IdOrderByEditedAtDesc(Long opportunityId);
}
