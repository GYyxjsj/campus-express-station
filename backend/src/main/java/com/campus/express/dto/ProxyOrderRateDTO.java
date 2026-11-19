package com.campus.express.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 代取服务评价入参 */
public class ProxyOrderRateDTO {
    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分 1-5")
    @Max(value = 5, message = "评分 1-5")
    private Integer rating;

    @Size(max = 255, message = "评价过长")
    private String review;

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
}