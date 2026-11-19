package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 兑换码 redeem_code */
public class RedeemCode {
    private Long id;
    private String code;
    private Long templateId;
    private String status;      // UNUSED/USED/DISABLED
    private Long usedBy;
    private OffsetDateTime usedAt;
    private OffsetDateTime createdAt;
    // 联查展示
    private String templateName;
    private String usedByName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getUsedBy() { return usedBy; }
    public void setUsedBy(Long usedBy) { this.usedBy = usedBy; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getUsedByName() { return usedByName; }
    public void setUsedByName(String usedByName) { this.usedByName = usedByName; }
}