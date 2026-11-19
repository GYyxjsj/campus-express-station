package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 取件提醒 notification */
public class Notification {
    private Long id;
    private Long userId;
    private Long parcelId;
    private String type;            // ARRIVE/REMIND/OVERDUE/PROXY_GRANTED/PROXY_USED/NOTICE
    private String channel;         // IN_APP/SMS/EMAIL
    private String title;
    private String content;
    private Integer isRead;
    private OffsetDateTime sentAt;
    private OffsetDateTime readAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
}