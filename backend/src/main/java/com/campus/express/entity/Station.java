package com.campus.express.entity;

import java.time.OffsetDateTime;

/** 驿站 station */
public class Station {
    private Long id;
    private String name;
    private String location;
    private String openHours;
    private String contact;
    private Integer status;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getOpenHours() { return openHours; }
    public void setOpenHours(String openHours) { this.openHours = openHours; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}