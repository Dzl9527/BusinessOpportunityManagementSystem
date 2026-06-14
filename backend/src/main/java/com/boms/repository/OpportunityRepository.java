package com.boms.repository;

import com.boms.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    Optional<Opportunity> findByReportFlowNo(String reportFlowNo);

    Optional<Opportunity> findByBidDocumentFlowNo(String bidDocumentFlowNo);

    @Query("SELECT SUM(o.value) FROM Opportunity o WHERE (:isAdmin = true OR (UPPER(o.visibilityStatus) != 'UNASSIGNED' AND (o.creatorUserId IN :userIds OR o.submitterUserId IN :userIds OR o.ownerUserId IN :userIds))) AND o.stage != 'lost'")
    Double sumValueByPermissions(@Param("isAdmin") Boolean isAdmin, @Param("userIds") List<String> userIds);

    @Query("SELECT COUNT(o) FROM Opportunity o WHERE (:isAdmin = true OR (UPPER(o.visibilityStatus) != 'UNASSIGNED' AND (o.creatorUserId IN :userIds OR o.submitterUserId IN :userIds OR o.ownerUserId IN :userIds))) AND o.stage != 'lost' AND o.stage != 'won'")
    Long countActiveByPermissions(@Param("isAdmin") Boolean isAdmin, @Param("userIds") List<String> userIds);

    @Query("SELECT o.stage, SUM(o.value) FROM Opportunity o WHERE (:isAdmin = true OR (UPPER(o.visibilityStatus) != 'UNASSIGNED' AND (o.creatorUserId IN :userIds OR o.submitterUserId IN :userIds OR o.ownerUserId IN :userIds))) GROUP BY o.stage")
    List<Object[]> getFunnelMetricsByPermissions(@Param("isAdmin") Boolean isAdmin, @Param("userIds") List<String> userIds);

    @Query("SELECT o.stage, COUNT(o) FROM Opportunity o WHERE (:isAdmin = true OR (UPPER(o.visibilityStatus) != 'UNASSIGNED' AND (o.creatorUserId IN :userIds OR o.submitterUserId IN :userIds OR o.ownerUserId IN :userIds))) GROUP BY o.stage")
    List<Object[]> getStageCountsByPermissions(@Param("isAdmin") Boolean isAdmin, @Param("userIds") List<String> userIds);

    @Query("SELECT SUBSTRING(o.closeDate, 1, 7) as month, SUM(o.value) FROM Opportunity o WHERE (:isAdmin = true OR (UPPER(o.visibilityStatus) != 'UNASSIGNED' AND (o.creatorUserId IN :userIds OR o.submitterUserId IN :userIds OR o.ownerUserId IN :userIds))) AND o.stage != 'lost' AND o.closeDate IS NOT NULL AND o.closeDate != '' GROUP BY SUBSTRING(o.closeDate, 1, 7) ORDER BY month ASC")
    List<Object[]> getTrendByPermissions(@Param("isAdmin") Boolean isAdmin, @Param("userIds") List<String> userIds);
}
