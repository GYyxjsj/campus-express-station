package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 服务站自寄单 ship_order */
public class ShipOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long stationId;
    private String courierCompany;
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String goodsType;
    private BigDecimal weightKg;
    private BigDecimal freight;
    private Integer insured;
    private String trackingNo;
    private String status;      // CREATED/DROPPED/SHIPPED/CANCELLED
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime droppedAt;
    private OffsetDateTime shippedAt;
    private OffsetDateTime cancelledAt;
    // 联查展示
    private String userName;
    private String stationName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public String getCourierCompany() { return courierCompany; }
    public void setCourierCompany(String courierCompany) { this.courierCompany = courierCompany; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }
    public String getGoodsType() { return goodsType; }
    public void setGoodsType(String goodsType) { this.goodsType = goodsType; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getFreight() { return freight; }
    public void setFreight(BigDecimal freight) { this.freight = freight; }
    public Integer getInsured() { return insured; }
    public void setInsured(Integer insured) { this.insured = insured; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getDroppedAt() { return droppedAt; }
    public void setDroppedAt(OffsetDateTime droppedAt) { this.droppedAt = droppedAt; }
    public OffsetDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(OffsetDateTime shippedAt) { this.shippedAt = shippedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
}