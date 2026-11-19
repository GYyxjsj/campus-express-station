package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 驿站创建/更新入参 */
public class StationDTO {
    @NotBlank(message = "驿站名称不能为空")
    @Size(max = 64)
    private String name;

    @NotBlank(message = "位置不能为空")
    @Size(max = 255)
    private String location;

    private String openHours = "08:00-21:00";
    private String contact;
    private Integer status = 1;

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
}