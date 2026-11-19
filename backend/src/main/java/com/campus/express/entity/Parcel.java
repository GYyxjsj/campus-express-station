package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 包裹 parcel */
public class Parcel {
    private Long id;
    private String trackingNo;
    private String courierCompany;
    private Long ownerId;
    private Long stationId;
    private Long shelfId;
    private String pickupCode;
    private String status;          // IN_STOCK / OVERDUE / PICKED_UP / RETURNED
    private String remark;
    private OffsetDateTime inboundTime;
    private OffsetDateTime outboundTime;
    private String returnReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public OffsetDateTime getInboundTime() { return inboundTime; }
    public void setInboundTime(OffsetDateTime inboundTime) { this.inboundTime = inboundTime; }
    public OffsetDateTime getOutboundTime() { return outboundTime; }
    public void setOutboundTime(OffsetDateTime outboundTime) { this.outboundTime = outboundTime; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}