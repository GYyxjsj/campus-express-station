package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 政策协议创建/更新入参 */
public class PolicyDTO {
    @NotBlank(message = "请填写协议编码")
    @Size(max = 32, message = "编码过长")
    private String code;

    @NotBlank(message = "请填写标题")
    @Size(max = 128, message = "标题过长")
    private String title;

    @NotBlank(message = "请填写版本号")
    @Size(max = 16, message = "版本号过长")
    private String version;

    private String category = "SERVICE";

    @Size(max = 255, message = "摘要过长")
    private String summary;

    @NotBlank(message = "请填写协议正文")
    private String content;

    private Integer required = 0;
    private Integer status = 1;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRequired() { return required; }
    public void setRequired(Integer required) { this.required = required; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}