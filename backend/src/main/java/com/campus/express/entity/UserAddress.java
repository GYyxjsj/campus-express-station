package com.campus.express.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 地址簿 user_address */
public class UserAddress {
    private Long id;
    private Long userId;
    private String label;
    private String contact;
    private String phone;
    private String region;
    private String detail;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer isDefault;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}