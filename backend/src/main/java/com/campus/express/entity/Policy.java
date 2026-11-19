package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 政策协议 policy */
public class Policy {
    private Long id;
    private String code;
    private String title;
    private String version;
    private String category;        // SERVICE/PRIVACY/PROXY/WALLET/CAMPUS
    private String summary;
    private String content;
    private Integer required;
    private Integer status;         // 1发布 0下线
    private OffsetDateTime effectiveAt;
    private Long publisherId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRequired() { return required; }
    public void setRequired(Integer required) { this.required = required; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public OffsetDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(OffsetDateTime effectiveAt) { this.effectiveAt = effectiveAt; }
    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}