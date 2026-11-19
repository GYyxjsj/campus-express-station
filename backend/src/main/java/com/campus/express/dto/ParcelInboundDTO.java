package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** 包裹入库入参 */
public class ParcelInboundDTO {
    @NotBlank(message = "运单号不能为空")
    @Size(max = 64, message = "运单号过长")
    private String trackingNo;

    @NotBlank(message = "快递公司不能为空")
    private String courierCompany;

    @NotBlank(message = "收件人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String ownerPhone;

    @NotNull(message = "请选择驿站")
    private Long stationId;

    private Long shelfId;

    @Size(max = 255, message = "备注过长")
    private String remark;

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}