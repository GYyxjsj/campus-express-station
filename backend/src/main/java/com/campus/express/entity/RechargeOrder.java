package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 充值订单 recharge_order */
public class RechargeOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal bonus;
    private String channel;         // CAMPUS_CARD/ALIPAY/WECHAT
    private String status;          // PENDING/PAID/CANCELLED/EXPIRED
    private OffsetDateTime createdAt;
    private OffsetDateTime expireAt;
    private OffsetDateTime paidAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
}