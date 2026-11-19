package com.campus.express.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 充值下单入参 */
public class RechargeDTO {
    @NotNull(message = "请输入充值金额")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "请选择支付渠道")
    private String channel;         // CAMPUS_CARD/ALIPAY/WECHAT

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}