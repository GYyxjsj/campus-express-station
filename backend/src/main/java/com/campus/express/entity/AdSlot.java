package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 广告位 ad_slot */
public class AdSlot {
    private Long id;
    private String slotKey;     // HOME_BANNER/PICKUP_SIDE/WALLET_FOOT
    private String title;
    private String content;
    private String linkUrl;
    private String theme;       // neutral/warm/cool
    private Integer weight;
    private Integer status;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlotKey() { return slotKey; }
    public void setSlotKey(String slotKey) { this.slotKey = slotKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}