package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 政策签署记录 policy_acceptance */
public class PolicyAcceptance {
    private Long id;
    private Long userId;
    private Long policyId;
    private String version;
    private String ip;
    private OffsetDateTime acceptedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}