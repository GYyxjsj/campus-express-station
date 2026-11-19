package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 收货偏好 receive_preference（每用户一行） */
public class ReceivePreference {
    private Long userId;
    private String timeSlot;        // ANY/MORNING/AFTERNOON/EVENING
    private String deliverMethod;   // SELF/PROXY/DOOR
    private String notifyChannel;   // IN_APP/SMS/EMAIL/ALL
    private String quietHours;
    private Integer noCall;
    private Integer fragileCare;
    private Integer autoProxy;
    private String note;
    private OffsetDateTime updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getDeliverMethod() { return deliverMethod; }
    public void setDeliverMethod(String deliverMethod) { this.deliverMethod = deliverMethod; }
    public String getNotifyChannel() { return notifyChannel; }
    public void setNotifyChannel(String notifyChannel) { this.notifyChannel = notifyChannel; }
    public String getQuietHours() { return quietHours; }
    public void setQuietHours(String quietHours) { this.quietHours = quietHours; }
    public Integer getNoCall() { return noCall; }
    public void setNoCall(Integer noCall) { this.noCall = noCall; }
    public Integer getFragileCare() { return fragileCare; }
    public void setFragileCare(Integer fragileCare) { this.fragileCare = fragileCare; }
    public Integer getAutoProxy() { return autoProxy; }
    public void setAutoProxy(Integer autoProxy) { this.autoProxy = autoProxy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}