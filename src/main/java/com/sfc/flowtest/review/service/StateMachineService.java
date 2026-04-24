package com.sfc.flowtest.review.service;

import com.sfc.flowtest.common.enums.ErrorCode;
import com.sfc.flowtest.common.enums.ManuscriptStatus;
import com.sfc.flowtest.common.exception.BizException;
import org.springframework.stereotype.Service;

/**
 * 审批状态机服务：
 * 统一维护状态解析与合法流转规则，避免业务规则散落。
 */
@Service
public class StateMachineService {

    /**
     * 将字符串状态码转换为枚举。
     *
     * @param rawStatus 原始状态码
     * @return 状态枚举
     */
    public ManuscriptStatus parseStatus(String rawStatus) {
        try {
            return ManuscriptStatus.valueOf(rawStatus);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.ILLEGAL_STATE, "未知状态: " + rawStatus);
        }
    }

    /**
     * 计算审核通过后的目标状态。
     *
     * @param current 当前状态
     * @return 下一状态
     */
    public ManuscriptStatus nextOnApprove(ManuscriptStatus current) {
        // 审核通过只能沿固定主链路前进，任何旁路状态都视为非法流转。
        return switch (current) {
            case PENDING_1ST -> ManuscriptStatus.PENDING_2ND;
            case PENDING_2ND -> ManuscriptStatus.PENDING_3RD;
            case PENDING_3RD -> ManuscriptStatus.COMPLETED;
            default -> throw new BizException(ErrorCode.ILLEGAL_STATE, "当前状态不允许审核通过: " + current);
        };
    }

    /**
     * 计算退回时的审核关卡（1/2/3）。
     *
     * @param current 当前状态
     * @return 退回关卡
     */
    public int rejectLevelOf(ManuscriptStatus current) {
        return switch (current) {
            case PENDING_1ST -> 1;
            case PENDING_2ND -> 2;
            case PENDING_3RD -> 3;
            default -> throw new BizException(ErrorCode.ILLEGAL_STATE, "当前状态不允许退回: " + current);
        };
    }

    /**
     * 根据退回关卡计算重提目标状态。
     *
     * @param rejectLevel 退回关卡
     * @return 重提目标状态
     */
    public ManuscriptStatus resubmitTarget(Integer rejectLevel) {
        if (rejectLevel == null) {
            throw new BizException(ErrorCode.REJECT_LEVEL_INVALID);
        }
        // 重提回“退回前的上一级环节”，与需求文档保持一致。
        return switch (rejectLevel) {
            case 1 -> ManuscriptStatus.DRAFT;
            case 2 -> ManuscriptStatus.PENDING_1ST;
            case 3 -> ManuscriptStatus.PENDING_2ND;
            default -> throw new BizException(ErrorCode.REJECT_LEVEL_INVALID, "非法退回关卡: " + rejectLevel);
        };
    }
}
