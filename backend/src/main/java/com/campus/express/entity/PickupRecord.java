package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 取件流水 pickup_record */
public class PickupRecord {
    private Long id;
    private Long parcelId;
    private String pickupType;      // SELF/PROXY
    private Long ownerId;
    private Long pickerId;
    private Long authId;
    private Long staffId;
    private Long stationId;
    private OffsetDateTime pickedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public String getPickupType() { return pickupType; }
    public void setPickupType(String pickupType) { this.pickupType = pickupType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public Long getPickerId() { return pickerId; }
    public void setPickerId(Long pickerId) { this.pickerId = pickerId; }
    public Long getAuthId() { return authId; }
    public void setAuthId(Long authId) { this.authId = authId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public OffsetDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(OffsetDateTime pickedAt) { this.pickedAt = pickedAt; }
}