package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/** 本人取件核销入参 */
public class PickupDTO {
    @NotNull(message = "包裹ID不能为空")
    private Long parcelId;

    @NotBlank(message = "取件码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "取件码须为6位数字")
    private String pickupCode;

    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
}