package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 超时保险单 insurance_policy */
public class InsurancePolicy {
    private Long id;
    private String policyNo;
    private Long userId;
    private Long parcelId;
    private BigDecimal premium;
    private BigDecimal coverage;
    private String status;      // ACTIVE/EXPIRED/CLAIMED/CLAIM_PAID
    private OffsetDateTime expireAt;
    private OffsetDateTime claimedAt;
    private OffsetDateTime settledAt;
    private OffsetDateTime createdAt;
    // 联查展示
    private String trackingNo;
    private String parcelStatus;
    private String courierCompany;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public BigDecimal getCoverage() { return coverage; }
    public void setCoverage(BigDecimal coverage) { this.coverage = coverage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public OffsetDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(OffsetDateTime claimedAt) { this.claimedAt = claimedAt; }
    public OffsetDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(OffsetDateTime settledAt) { this.settledAt = settledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getParcelStatus() { return parcelStatus; }
    public void setParcelStatus(String parcelStatus) { this.parcelStatus = parcelStatus; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
}