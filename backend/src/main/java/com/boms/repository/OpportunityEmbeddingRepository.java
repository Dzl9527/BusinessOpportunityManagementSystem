package com.boms.repository;

import com.boms.model.OpportunityEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityEmbeddingRepository extends JpaRepository<OpportunityEmbedding, Long> {
}
