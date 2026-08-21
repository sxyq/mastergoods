package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "poster_generations")
public class PosterGenerationEntity {
    @Id
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "prompt_text", nullable = false)
    private String promptText;

    @Column(name = "reference_image_asset_ids")
    private String referenceImageAssetIds;

    @Column(name = "result_image_url")
    private String resultImageUrl;

    @Column(nullable = false)
    private String status;

    @Column(name = "credits_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditsCost;

    @Column(nullable = false)
    private Integer iteration;

    @Column(name = "parent_generation_id")
    private Long parentGenerationId;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getReferenceImageAssetIds() {
        return referenceImageAssetIds;
    }

    public void setReferenceImageAssetIds(String referenceImageAssetIds) {
        this.referenceImageAssetIds = referenceImageAssetIds;
    }

    public String getResultImageUrl() {
        return resultImageUrl;
    }

    public void setResultImageUrl(String resultImageUrl) {
        this.resultImageUrl = resultImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getCreditsCost() {
        return creditsCost;
    }

    public void setCreditsCost(BigDecimal creditsCost) {
        this.creditsCost = creditsCost;
    }

    public Integer getIteration() {
        return iteration;
    }

    public void setIteration(Integer iteration) {
        this.iteration = iteration;
    }

    public Long getParentGenerationId() {
        return parentGenerationId;
    }

    public void setParentGenerationId(Long parentGenerationId) {
        this.parentGenerationId = parentGenerationId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
