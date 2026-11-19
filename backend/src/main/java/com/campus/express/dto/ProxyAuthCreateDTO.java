package com.campus.express.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 发起代取授权入参 */
public class ProxyAuthCreateDTO {
    @NotNull(message = "请选择包裹")
    private Long parcelId;

    @NotBlank(message = "请填写被授权人学号")
    private String granteeUsername;

    @NotNull(message = "请填写有效期")
    @Min(value = 1, message = "有效期至少1小时")
    @Max(value = 72, message = "有效期最长72小时")
    private Integer validHours;

    @Size(max = 255, message = "备注过长")
    private String remark;

    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public String getGranteeUsername() { return granteeUsername; }
    public void setGranteeUsername(String granteeUsername) { this.granteeUsername = granteeUsername; }
    public Integer getValidHours() { return validHours; }
    public void setValidHours(Integer validHours) { this.validHours = validHours; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}