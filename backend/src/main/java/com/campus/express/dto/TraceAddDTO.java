package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 去向登记入参：检查/移架/备注等人工节点 */
public class TraceAddDTO {
    @NotNull(message = "请选择包裹")
    private Long parcelId;

    @NotBlank(message = "请选择节点类型")
    private String node;            // INSPECT/TRANSFER/SHELVE/NOTE

    @NotBlank(message = "请填写地点")
    @Size(max = 255, message = "地点过长")
    private String location;

    @Size(max = 128, message = "标题过长")
    private String title;

    @Size(max = 512, message = "详情过长")
    private String detail;

    private Long shelfId;           // TRANSFER 时目标货架

    public Long getParcelId() { return parcelId; }
    public void setParcelId(Long parcelId) { this.parcelId = parcelId; }
    public String getNode() { return node; }
    public void setNode(String node) { this.node = node; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
}