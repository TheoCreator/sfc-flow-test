package com.sfc.flowtest.review.vo;

import java.time.LocalDateTime;

/**
 * 审核流水响应对象。
 */
public class ReviewRecordVO {
    private String action;
    private String fromStatus;
    private String toStatus;
    private String opinion;
    private Integer rejectLevel;
    private String operatorName;
    private LocalDateTime createdAt;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Integer getRejectLevel() {
        return rejectLevel;
    }

    public void setRejectLevel(Integer rejectLevel) {
        this.rejectLevel = rejectLevel;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
