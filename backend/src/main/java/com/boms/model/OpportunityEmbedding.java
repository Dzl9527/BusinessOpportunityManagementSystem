package com.boms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "opportunity_embeddings")
public class OpportunityEmbedding {

    @Id
    private Long opportunityId;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String embeddingJson;

    @Column(length = 100)
    private String textMd5;

    public OpportunityEmbedding() {
    }

    public OpportunityEmbedding(Long opportunityId, String embeddingJson, String textMd5) {
        this.opportunityId = opportunityId;
        this.embeddingJson = embeddingJson;
        this.textMd5 = textMd5;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
    }

    public String getEmbeddingJson() {
        return embeddingJson;
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }

    public String getTextMd5() {
        return textMd5;
    }

    public void setTextMd5(String textMd5) {
        this.textMd5 = textMd5;
    }
}
