package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/** 管理员账务调整入参（amount 带符号） */
public class WalletAdjustDTO {
    @NotNull(message = "请选择用户")
    private Long userId;

    @NotNull(message = "请输入调整金额")
    private BigDecimal amount;

    @NotBlank(message = "请填写调整事由")
    @Size(max = 255, message = "事由过长")
    private String remark;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}