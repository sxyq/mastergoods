package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "inventory_monthly_stats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_monthly_stats_owner_product_period", columnNames = {"owner_user_id", "product_id", "year", "month"})
    }
)
public class InventoryMonthlyStatsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_code", nullable = false, length = 64)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "quantity_in", nullable = false)
    private Double quantityIn;

    @Column(name = "quantity_out", nullable = false)
    private Double quantityOut;

    @Column(name = "quantity_adjust", nullable = false)
    private Double quantityAdjust;

    @Column(name = "quantity_begin", nullable = false)
    private Double quantityBegin;

    @Column(name = "quantity_end", nullable = false)
    private Double quantityEnd;

    @Column(name = "total_cost_in", nullable = false)
    private Double totalCostIn;

    @Column(name = "total_cost_out", nullable = false)
    private Double totalCostOut;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Double getQuantityIn() { return quantityIn; }
    public void setQuantityIn(Double quantityIn) { this.quantityIn = quantityIn; }
    public Double getQuantityOut() { return quantityOut; }
    public void setQuantityOut(Double quantityOut) { this.quantityOut = quantityOut; }
    public Double getQuantityAdjust() { return quantityAdjust; }
    public void setQuantityAdjust(Double quantityAdjust) { this.quantityAdjust = quantityAdjust; }
    public Double getQuantityBegin() { return quantityBegin; }
    public void setQuantityBegin(Double quantityBegin) { this.quantityBegin = quantityBegin; }
    public Double getQuantityEnd() { return quantityEnd; }
    public void setQuantityEnd(Double quantityEnd) { this.quantityEnd = quantityEnd; }
    public Double getTotalCostIn() { return totalCostIn; }
    public void setTotalCostIn(Double totalCostIn) { this.totalCostIn = totalCostIn; }
    public Double getTotalCostOut() { return totalCostOut; }
    public void setTotalCostOut(Double totalCostOut) { this.totalCostOut = totalCostOut; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
