package com.campus.express.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 公告创建/更新入参 */
public class NoticeDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题过长")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 2048, message = "内容过长")
    private String content;

    private Integer status = 1;
    private Integer isPopup = 0;
    private Integer priority = 0;
    private String category = "NOTICE";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsPopup() { return isPopup; }
    public void setIsPopup(Integer isPopup) { this.isPopup = isPopup; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}