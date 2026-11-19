package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** 代取核销入参 */
public class ProxyPickupDTO {
    @NotBlank(message = "授权码不能为空")
    @Pattern(regexp = "^[A-Z2-9]{8}$", message = "授权码须为8位大写字母数字")
    private String authCode;

    @NotBlank(message = "取件码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "取件码须为6位数字")
    private String pickupCode;

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }
}