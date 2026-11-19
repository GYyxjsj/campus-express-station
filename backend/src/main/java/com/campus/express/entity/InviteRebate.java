package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 邀请充值返利 invite_rebate */
public class InviteRebate {
    private Long id;
    private Long inviterId;
    private Long inviteeId;
    private Long rechargeOrderId;
    private BigDecimal rechargeAmount;
    private BigDecimal ratePercent;
    private BigDecimal amount;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }
    public Long getInviteeId() { return inviteeId; }
    public void setInviteeId(Long inviteeId) { this.inviteeId = inviteeId; }
    public Long getRechargeOrderId() { return rechargeOrderId; }
    public void setRechargeOrderId(Long rechargeOrderId) { this.rechargeOrderId = rechargeOrderId; }
    public BigDecimal getRechargeAmount() { return rechargeAmount; }
    public void setRechargeAmount(BigDecimal rechargeAmount) { this.rechargeAmount = rechargeAmount; }
    public BigDecimal getRatePercent() { return ratePercent; }
    public void setRatePercent(BigDecimal ratePercent) { this.ratePercent = ratePercent; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}