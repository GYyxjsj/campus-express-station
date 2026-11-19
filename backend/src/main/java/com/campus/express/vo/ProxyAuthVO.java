package com.campus.express.vo;

import com.campus.express.entity.ProxyAuth;

/** 代取授权 VO（联查包裹与双方姓名） */
public class ProxyAuthVO extends ProxyAuth {
    private String trackingNo;
    private String courierCompany;
    private String parcelStatus;
    private String stationName;
    private String shelfCode;
    private String grantorName;
    private String grantorUsername;
    private String granteeName;
    private String granteeUsername;

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
    public String getParcelStatus() { return parcelStatus; }
    public void setParcelStatus(String parcelStatus) { this.parcelStatus = parcelStatus; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }
    public String getGrantorName() { return grantorName; }
    public void setGrantorName(String grantorName) { this.grantorName = grantorName; }
    public String getGrantorUsername() { return grantorUsername; }
    public void setGrantorUsername(String grantorUsername) { this.grantorUsername = grantorUsername; }
    public String getGranteeName() { return granteeName; }
    public void setGranteeName(String granteeName) { this.granteeName = granteeName; }
    public String getGranteeUsername() { return granteeUsername; }
    public void setGranteeUsername(String granteeUsername) { this.granteeUsername = granteeUsername; }
}