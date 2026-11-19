package com.campus.express.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/** 货架创建入参 */
public class ShelfDTO {
    @NotNull(message = "请选择驿站")
    private Long stationId;

    @NotBlank(message = "货架编码不能为空")
    @Pattern(regexp = "^[A-Z]-\\d{2}-\\d{2}$", message = "编码格式如 A-01-03")
    private String code;

    @Min(value = 1, message = "容量至少为1")
    private Integer capacity = 40;

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}