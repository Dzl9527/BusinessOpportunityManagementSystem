package com.boms.specification;

import com.boms.model.Opportunity;
import com.boms.model.SystemUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpportunitySpec {

    public static Specification<Opportunity> filterBy(Map<String, String> params, boolean isAdmin, Set<String> visibleUserIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search keyword
            String search = params.get("search");
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern);
                Predicate companyLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), pattern);
                Predicate supplierLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("supplierCompany")), pattern);
                Predicate modelLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("deviceModels")), pattern);
                predicates.add(criteriaBuilder.or(nameLike, companyLike, supplierLike, modelLike));
            }

            // 2. Exact match fields
            addExactMatch(predicates, criteriaBuilder, root, "stage", params.get("stage"));
            addExactMatch(predicates, criteriaBuilder, root, "priority", params.get("priority"));
            addExactMatch(predicates, criteriaBuilder, root, "owner", params.get("owner"));
            addExactMatch(predicates, criteriaBuilder, root, "industry", params.get("industry"));
            addExactMatch(predicates, criteriaBuilder, root, "submitterRegion", params.get("submitterRegion"));
            addExactMatch(predicates, criteriaBuilder, root, "supplyRegion", params.get("supplyRegion"));
            addExactMatch(predicates, criteriaBuilder, root, "purchaseType", params.get("purchaseType"));
            addExactMatch(predicates, criteriaBuilder, root, "winRateLabel", params.get("winRateLabel"));
            addExactMatch(predicates, criteriaBuilder, root, "businessProgressStatus", params.get("businessProgressStatus"));
            addExactMatch(predicates, criteriaBuilder, root, "submitter", params.get("submitter"));
            addExactMatch(predicates, criteriaBuilder, root, "source", params.get("source"));

            String closeDateStart = params.get("closeDateStart");
            String closeDateEnd = params.get("closeDateEnd");
            if (closeDateStart != null && !closeDateStart.trim().isEmpty()) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("closeDate"), closeDateStart.trim()));
            }
            if (closeDateEnd != null && !closeDateEnd.trim().isEmpty()) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("closeDate"), closeDateEnd.trim()));
            }

            String startDate = params.get("startDate");
            String endDate = params.get("endDate");
            if (startDate != null && !startDate.trim().isEmpty()) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("submitDate"), startDate.trim()));
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("submitDate"), endDate.trim()));
            }

            String ownerOrSubmitter = params.get("ownerOrSubmitter");
            if (ownerOrSubmitter != null && !ownerOrSubmitter.trim().isEmpty()) {
                String p = "%" + ownerOrSubmitter.trim().toLowerCase() + "%";
                Predicate mo = criteriaBuilder.like(criteriaBuilder.lower(root.get("owner")), p);
                Predicate ms = criteriaBuilder.like(criteriaBuilder.lower(root.get("submitter")), p);
                predicates.add(criteriaBuilder.or(mo, ms));
            }

            // 3. Boolean match fields
            addBooleanMatch(predicates, criteriaBuilder, root, "bidWon", params.get("bidWon"));
            addBooleanMatch(predicates, criteriaBuilder, root, "reportedSuccessfully", params.get("reportedSuccessfully"));

            // 4. Permissions (Database Level)
            if (!isAdmin) {
                Predicate notUnassigned = criteriaBuilder.notEqual(criteriaBuilder.upper(root.get("visibilityStatus")), "UNASSIGNED");
                
                if (visibleUserIds == null || visibleUserIds.isEmpty()) {
                    predicates.add(criteriaBuilder.disjunction()); // Always false
                } else {
                    Predicate matchCreator = root.get("creatorUserId").in(visibleUserIds);
                    Predicate matchSubmitter = root.get("submitterUserId").in(visibleUserIds);
                    Predicate matchOwner = root.get("ownerUserId").in(visibleUserIds);
                    Predicate matchAnyUser = criteriaBuilder.or(matchCreator, matchSubmitter, matchOwner);
                    
                    predicates.add(criteriaBuilder.and(notUnassigned, matchAnyUser));
                }
            }

            // 5. Active only (for Kanban)
            String activeOnly = params.get("activeOnly");
            if ("true".equalsIgnoreCase(activeOnly)) {
                predicates.add(criteriaBuilder.notEqual(root.get("stage"), "won"));
                predicates.add(criteriaBuilder.notEqual(root.get("stage"), "lost"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addExactMatch(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Opportunity> root, String fieldName, String value) {
        if (value != null && !value.equals("all") && !value.trim().isEmpty()) {
            predicates.add(cb.equal(root.get(fieldName), value));
        }
    }

    private static void addBooleanMatch(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Opportunity> root, String fieldName, String value) {
        if (value != null && !value.equals("all") && !value.trim().isEmpty()) {
            if ("true".equalsIgnoreCase(value) || "是".equals(value)) {
                predicates.add(cb.isTrue(root.get(fieldName)));
            } else if ("false".equalsIgnoreCase(value) || "否".equals(value)) {
                predicates.add(cb.isFalse(root.get(fieldName)));
            }
        }
    }
}
