package com.sfc.flowtest.review.dto;

import jakarta.validation.constraints.Size;

/**
 * 审核退回请求参数。
 */
public class RejectRequest {
    @Size(max = 2000, message = "退回意见长度不能超过2000")
    private String opinion;

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }
}
