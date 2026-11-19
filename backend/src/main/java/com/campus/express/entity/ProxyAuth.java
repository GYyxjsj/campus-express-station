package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 代取授权 proxy_auth */
public class ProxyAuth {
    private Long id;
    private Long parcelId;
    private Long grantorId;         // 授权人(物主)
    private Long granteeId;         // 被授权人(代取人)
    private String authCode;
    private String status;          // ACTIVE/USED/REVOKED/EXPIRED
    private OffsetDateTime expireAt;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime usedAt;
    private OffsetDateTime revokedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public Long getGrantorId() { return grantorId; }
    public void setGrantorId(Long grantorId) { this.grantorId = grantorId; }
    public Long getGranteeId() { return granteeId; }
    public void setGranteeId(Long granteeId) { this.granteeId = granteeId; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(OffsetDateTime revokedAt) { this.revokedAt = revokedAt; }
}