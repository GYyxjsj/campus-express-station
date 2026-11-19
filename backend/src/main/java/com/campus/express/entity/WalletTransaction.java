package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 钱包流水 wallet_transaction（amount 带符号：收入为正，支出为负） */
public class WalletTransaction {
    private Long id;
    private Long userId;
    private String type;            // RECHARGE/CONSUME/REFUND/EARN/ADJUST
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String channel;         // CAMPUS_CARD/ALIPAY/WECHAT/BALANCE/SYSTEM
    private String title;
    private String refType;         // RECHARGE_ORDER/PROXY_ORDER/PARCEL/ADMIN
    private Long refId;
    private String remark;
    private Long operatorId;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}