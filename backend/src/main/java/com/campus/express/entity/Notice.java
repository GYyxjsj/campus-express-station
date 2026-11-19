package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 公告 notice */
public class Notice {
    private Long id;
    private String title;
    private String content;
    private Integer status;         // 1发布 0下线
    private Integer isPopup;        // 1登录后弹窗
    private Integer priority;       // 0普通 1重要 2紧急
    private String category;        // NOTICE/OPERATION/FEATURE/RULE
    private Long publisherId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsPopup() { return isPopup; }
    public void setIsPopup(Integer isPopup) { this.isPopup = isPopup; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}