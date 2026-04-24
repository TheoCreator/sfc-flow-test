package com.sfc.flowtest.review.service;

import com.sfc.flowtest.common.enums.ReviewAction;
import com.sfc.flowtest.review.entity.MsReviewRecord;
import com.sfc.flowtest.review.mapper.MsReviewRecordMapper;
import com.sfc.flowtest.review.vo.ReviewRecordVO;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 审核流水服务：
 * 负责流水写入和展示对象转换。
 */
@Service
public class ReviewRecordService {
    private static final Logger log = LoggerFactory.getLogger(ReviewRecordService.class);
    private static final Long DEFAULT_OPERATOR_ID = 1L;
    private static final String DEFAULT_OPERATOR_NAME = "system";

    private final MsReviewRecordMapper reviewRecordMapper;

    public ReviewRecordService(MsReviewRecordMapper reviewRecordMapper) {
        this.reviewRecordMapper = reviewRecordMapper;
    }

    /**
     * 追加一条审核流水。
     *
     * @param manuscriptId 稿件ID
     * @param action       动作
     * @param fromStatus   原状态
     * @param toStatus     新状态
     * @param opinion      审核意见
     * @param rejectLevel  退回关卡
     */
    public void append(Long manuscriptId,
                       ReviewAction action,
                       String fromStatus,
                       String toStatus,
                       String opinion,
                       Integer rejectLevel) {
        MsReviewRecord record = new MsReviewRecord();
        record.setManuscriptId(manuscriptId);
        record.setAction(action.name());
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setOpinion(opinion);
        record.setRejectLevel(rejectLevel);
        record.setOperatorId(DEFAULT_OPERATOR_ID);
        record.setOperatorName(DEFAULT_OPERATOR_NAME);
        reviewRecordMapper.insert(record);
        log.info("manuscriptId={}, action={}, fromStatus={}, toStatus={}",
                manuscriptId, action.name(), fromStatus, toStatus);
    }

    /**
     * 查询并转换稿件审核流水。
     *
     * @param manuscriptId 稿件ID
     * @return 流水展示列表
     */
    public List<ReviewRecordVO> listByManuscriptId(Long manuscriptId) {
        return reviewRecordMapper.selectByManuscriptId(manuscriptId).stream().map(record -> {
            ReviewRecordVO vo = new ReviewRecordVO();
            vo.setAction(record.getAction());
            vo.setFromStatus(record.getFromStatus());
            vo.setToStatus(record.getToStatus());
            vo.setOpinion(record.getOpinion());
            vo.setRejectLevel(record.getRejectLevel());
            vo.setOperatorName(record.getOperatorName());
            vo.setCreatedAt(record.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }
}
