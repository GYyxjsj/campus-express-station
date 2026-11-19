package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 电话簿 phone_contact */
public class PhoneContact {
    private Long id;
    private Long userId;
    private String name;
    private String phone;
    private String relation;
    private Integer isStarred;
    private String remark;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }
    public Integer getIsStarred() { return isStarred; }
    public void setIsStarred(Integer isStarred) { this.isStarred = isStarred; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}