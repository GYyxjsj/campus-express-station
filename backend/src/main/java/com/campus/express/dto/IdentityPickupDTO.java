package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 柜台凭身份条码取件入参 */
public class IdentityPickupDTO {
    @NotBlank(message = "请扫描或输入身份条码")
    private String identityCode;

    @NotNull(message = "请选择包裹")
    private Long parcelId;

    public String getIdentityCode() { return identityCode; }
    public void setIdentityCode(String identityCode) { this.identityCode = identityCode; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
}