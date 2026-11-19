package com.campus.express.entity;

/** 货架 shelf */
public class Shelf {
    private Long id;
    private Long stationId;
    private String code;
    private Integer capacity;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}