package com.campus.express.vo;

import com.campus.express.entity.Parcel;

/** 包裹列表/详情 VO（联查物主、驿站、货架、生效授权） */
public class ParcelVO extends Parcel {
    private String ownerName;
    private String ownerUsername;
    private String ownerPhone;
    private String stationName;
    private String shelfCode;
    private Long activeAuthId;        // 生效中的代取授权 id（无则 null）
    private String activeAuthGrantee; // 被授权人姓名

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }
    public Long getActiveAuthId() { return activeAuthId; }
    public void setActiveAuthId(Long activeAuthId) { this.activeAuthId = activeAuthId; }
    public String getActiveAuthGrantee() { return activeAuthGrantee; }
    public void setActiveAuthGrantee(String activeAuthGrantee) { this.activeAuthGrantee = activeAuthGrantee; }
}