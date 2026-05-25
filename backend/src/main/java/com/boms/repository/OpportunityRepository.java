package com.boms.repository;

import com.boms.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    Optional<Opportunity> findByReportFlowNo(String reportFlowNo);

    Optional<Opportunity> findByBidDocumentFlowNo(String bidDocumentFlowNo);
}
