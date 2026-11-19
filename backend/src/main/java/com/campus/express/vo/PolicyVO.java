package com.campus.express.vo;

import com.campus.express.entity.Policy;

import java.time.OffsetDateTime;

/** 政策协议 VO（附当前用户签署状态与签署人数） */
public class PolicyVO extends Policy {
    private Boolean accepted;
    private OffsetDateTime acceptedAt;
    private String acceptedVersion;
    private Long acceptCount;
    private String publisherName;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getAcceptedVersion() { return acceptedVersion; }
    public void setAcceptedVersion(String acceptedVersion) { this.acceptedVersion = acceptedVersion; }
    public Long getAcceptCount() { return acceptCount; }
    public void setAcceptCount(Long acceptCount) { this.acceptCount = acceptCount; }
    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }
}