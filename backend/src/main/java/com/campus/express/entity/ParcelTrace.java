package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 包裹去向轨迹 parcel_trace（时间 + 地点 + 检查员/经办人） */
public class ParcelTrace {
    private Long id;
    private Long parcelId;
    private String node;            // ARRIVE/SHELVE/INSPECT/TRANSFER/REMIND/OVERDUE/AUTH_GRANTED/AUTH_REVOKED/PICKUP/PICKUP_IDENTITY/RETURN/SERVICE_*
    private String title;
    private String location;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;    // COURIER/STAFF/ADMIN/STUDENT/SYSTEM
    private String detail;
    private OffsetDateTime happenedAt;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public String getNode() { return node; }
    public void setNode(String node) { this.node = node; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public OffsetDateTime getHappenedAt() { return happenedAt; }
    public void setHappenedAt(OffsetDateTime happenedAt) { this.happenedAt = happenedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}