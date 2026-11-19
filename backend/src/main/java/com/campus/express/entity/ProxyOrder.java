package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 代取服务单 proxy_order（有偿跑腿，托管→接单→取件→送达→确认结算） */
public class ProxyOrder {
    private Long id;
    private String orderNo;
    private Long parcelId;
    private Long requesterId;
    private Long runnerId;
    private Long authId;
    private BigDecimal fee;
    private BigDecimal tip;
    private BigDecimal commission;
    private String status;          // OPEN/ACCEPTED/PICKED/DELIVERED/DONE/CANCELLED/EXPIRED
    private String deliverTo;
    private String note;
    private OffsetDateTime expireAt;
    private Integer rating;
    private String review;
    private OffsetDateTime createdAt;
    private OffsetDateTime acceptedAt;
    private OffsetDateTime pickedAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime doneAt;
    private OffsetDateTime cancelledAt;
    private String cancelReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
    public Long getRunnerId() { return runnerId; }
    public void setRunnerId(Long runnerId) { this.runnerId = runnerId; }
    public Long getAuthId() { return authId; }
    public void setAuthId(Long authId) { this.authId = authId; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getTip() { return tip; }
    public void setTip(BigDecimal tip) { this.tip = tip; }
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDeliverTo() { return deliverTo; }
    public void setDeliverTo(String deliverTo) { this.deliverTo = deliverTo; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public OffsetDateTime getPickedAt() { return pickedAt; }
    public void setPickedAt(OffsetDateTime pickedAt) { this.pickedAt = pickedAt; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public OffsetDateTime getDoneAt() { return doneAt; }
    public void setDoneAt(OffsetDateTime doneAt) { this.doneAt = doneAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}