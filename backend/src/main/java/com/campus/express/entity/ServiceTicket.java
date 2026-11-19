package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 客服工单 service_ticket */
public class ServiceTicket {
    private Long id;
    private String ticketNo;
    private Long userId;
    private String category;    // PARCEL/PROXY/WALLET/SHIP/OTHER
    private String title;
    private String content;
    private String contact;
    private String status;      // PENDING/PROCESSING/DONE
    private Integer isRead;
    private Long handlerId;
    private OffsetDateTime handledAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    // 联查展示
    private String userName;
    private String userPhone;
    private String handlerName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }
    public Long getHandlerId() { return handlerId; }
    public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
    public OffsetDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(OffsetDateTime handledAt) { this.handledAt = handledAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getHandlerName() { return handlerName; }
    public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
}