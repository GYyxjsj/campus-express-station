package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 卡券模板 coupon_template */
public class CouponTemplate {
    private Long id;
    private String name;
    private String type;        // COUPON 优惠券 / RED_PACKET 红包
    private BigDecimal amount;
    private BigDecimal minSpend;
    private String scope;       // ALL/SHIP/PROXY/INSURANCE
    private Integer totalQty;
    private Integer claimedQty;
    private Integer validDays;
    private String description;
    private Integer status;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getMinSpend() { return minSpend; }
    public void setMinSpend(BigDecimal minSpend) { this.minSpend = minSpend; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Integer getTotalQty() { return totalQty; }
    public void setTotalQty(Integer totalQty) { this.totalQty = totalQty; }
    public Integer getClaimedQty() { return claimedQty; }
    public void setClaimedQty(Integer claimedQty) { this.claimedQty = claimedQty; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}