package com.campus.express.dto;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/** 批量签署协议入参 */
public class PolicyAcceptDTO {
    @NotEmpty(message = "请选择要签署的协议")
    private List<Long> policyIds;

    public List<Long> getPolicyIds() { return policyIds; }
    public void setPolicyIds(List<Long> policyIds) { this.policyIds = policyIds; }
}