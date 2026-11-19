package com.campus.express.vo;

import com.campus.express.entity.ProxyOrder;

import java.math.BigDecimal;

/** 代取服务单 VO（联查包裹、双方、驿站、授权码） */
public class ProxyOrderVO extends ProxyOrder {
    private String trackingNo;
    private String courierCompany;
    private String parcelStatus;
    private String stationName;
    private String stationLocation;
    private String shelfCode;
    private String pickupCode;
    private String requesterName;
    private String requesterUsername;
    private String requesterPhone;
    private String runnerName;
    private String runnerUsername;
    private String runnerPhone;
    private String authCode;
    private BigDecimal total;
    private BigDecimal runnerIncome;

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
    public String getParcelStatus() { return parcelStatus; }
    public void setParcelStatus(String parcelStatus) { this.parcelStatus = parcelStatus; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getStationLocation() { return stationLocation; }
    public void setStationLocation(String stationLocation) { this.stationLocation = stationLocation; }
    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }
    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }
    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
    public String getRunnerName() { return runnerName; }
    public void setRunnerName(String runnerName) { this.runnerName = runnerName; }
    public String getRunnerUsername() { return runnerUsername; }
    public void setRunnerUsername(String runnerUsername) { this.runnerUsername = runnerUsername; }
    public String getRunnerPhone() { return runnerPhone; }
    public void setRunnerPhone(String runnerPhone) { this.runnerPhone = runnerPhone; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getRunnerIncome() { return runnerIncome; }
    public void setRunnerIncome(BigDecimal runnerIncome) { this.runnerIncome = runnerIncome; }
}