package com.campus.express.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/** 发布代取服务单入参 */
public class ProxyOrderCreateDTO {
    @NotNull(message = "请选择包裹")
    private Long parcelId;

    @NotNull(message = "请输入服务费")
    @DecimalMin(value = "0", message = "服务费不能为负")
    private BigDecimal fee;

    @DecimalMin(value = "0", message = "小费不能为负")
    private BigDecimal tip = BigDecimal.ZERO;

    @NotBlank(message = "请填写送达地点")
    @Size(max = 255, message = "送达地点过长")
    private String deliverTo;

    @Size(max = 255, message = "备注过长")
    private String note;

    private Integer hours = 24;

    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getTip() { return tip; }
    public void setTip(BigDecimal tip) { this.tip = tip; }
    public String getDeliverTo() { return deliverTo; }
    public void setDeliverTo(String deliverTo) { this.deliverTo = deliverTo; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Integer getHours() { return hours; }
    public void setHours(Integer hours) { this.hours = hours; }
}