package com.campus.express.vo;

import com.campus.express.entity.ParcelTrace;

/** 去向轨迹 VO（联查包裹与驿站） */
public class ParcelTraceVO extends ParcelTrace {
    private String trackingNo;
    private String courierCompany;
    private String parcelStatus;
    private String stationName;
    private String ownerName;

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
    public String getParcelStatus() { return parcelStatus; }
    public void setParcelStatus(String parcelStatus) { this.parcelStatus = parcelStatus; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}