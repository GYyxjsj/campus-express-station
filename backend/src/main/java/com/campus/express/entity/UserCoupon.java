package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 用户卡券 user_coupon（联查模板字段供展示） */
public class UserCoupon {
    private Long id;
    private Long templateId;
    private Long userId;
    private String code;
    private String status;      // UNUSED/USED/EXPIRED
    private String source;      // CLAIM/REDEEM/SYSTEM
    private OffsetDateTime expireAt;
    private OffsetDateTime usedAt;
    private OffsetDateTime createdAt;
    // 联查展示字段
    private String name;
    private String type;
    private BigDecimal amount;
    private BigDecimal minSpend;
    private String scope;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}