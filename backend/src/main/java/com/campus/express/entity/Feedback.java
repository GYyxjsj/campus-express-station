package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 意见反馈 feedback */
public class Feedback {
    private Long id;
    private Long userId;
    private String type;        // SUGGEST/BUG/COMPLAINT/PRAISE
    private String content;
    private String contact;
    private String status;      // PENDING/REPLIED
    private String reply;
    private Long replierId;
    private OffsetDateTime repliedAt;
    private OffsetDateTime createdAt;
    // 联查展示
    private String userName;
    private String replierName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public Long getReplierId() { return replierId; }
    public void setReplierId(Long replierId) { this.replierId = replierId; }
    public OffsetDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(OffsetDateTime repliedAt) { this.repliedAt = repliedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getReplierName() { return replierName; }
    public void setReplierName(String replierName) { this.replierName = replierName; }
}