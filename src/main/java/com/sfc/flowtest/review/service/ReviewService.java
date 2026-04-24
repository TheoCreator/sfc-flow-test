package com.sfc.flowtest.review.service;

import com.sfc.flowtest.common.enums.ErrorCode;
import com.sfc.flowtest.common.enums.ManuscriptStatus;
import com.sfc.flowtest.common.enums.ReviewAction;
import com.sfc.flowtest.common.exception.BizException;
import com.sfc.flowtest.manuscript.entity.MsManuscript;
import com.sfc.flowtest.manuscript.mapper.MsManuscriptMapper;
import com.sfc.flowtest.manuscript.service.ManuscriptService;
import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;
import com.sfc.flowtest.publish.event.ManuscriptCompletedEvent;
import com.sfc.flowtest.review.vo.ReviewRecordVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 审核领域服务：
 * 负责审核通过、退回与审核流水查询。
 */
@Service
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ManuscriptService manuscriptService;
    private final MsManuscriptMapper manuscriptMapper;
    private final StateMachineService stateMachineService;
    private final ReviewRecordService reviewRecordService;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewService(ManuscriptService manuscriptService,
                         MsManuscriptMapper manuscriptMapper,
                         StateMachineService stateMachineService,
                         ReviewRecordService reviewRecordService,
                         ApplicationEventPublisher eventPublisher) {
        this.manuscriptService = manuscriptService;
        this.manuscriptMapper = manuscriptMapper;
        this.stateMachineService = stateMachineService;
        this.reviewRecordService = reviewRecordService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 审核通过，按状态机推进到下一节点。
     *
     * @param id 稿件ID
     * @return 更新后的稿件详情
     */
    @Transactional
    public ManuscriptDetailVO approve(Long id) {
        MsManuscript manuscript = manuscriptService.requireById(id);
        ManuscriptStatus current = stateMachineService.parseStatus(manuscript.getStatus());
        ManuscriptStatus next = stateMachineService.nextOnApprove(current);
        int affected = manuscriptMapper.updateStatus(id, next.name(), null);
        ensureSingleRowUpdated(affected, id, "approve");
        reviewRecordService.append(id, ReviewAction.APPROVE, current.name(), next.name(), null, null);
        log.info("manuscriptId={}, action={}, fromStatus={}, toStatus={}", id, ReviewAction.APPROVE.name(), current.name(), next.name());
        if (next == ManuscriptStatus.COMPLETED) {
            eventPublisher.publishEvent(new ManuscriptCompletedEvent(id));
        }
        return manuscriptService.detail(id);
    }

    /**
     * 审核退回，意见必填，状态统一进入 REJECTED。
     *
     * @param id      稿件ID
     * @param opinion 退回意见
     * @return 更新后的稿件详情
     */
    @Transactional
    public ManuscriptDetailVO reject(Long id, String opinion) {
        if (!StringUtils.hasText(opinion)) {
            throw new BizException(ErrorCode.REJECT_OPINION_REQUIRED);
        }
        MsManuscript manuscript = manuscriptService.requireById(id);
        ManuscriptStatus current = stateMachineService.parseStatus(manuscript.getStatus());
        int rejectLevel = stateMachineService.rejectLevelOf(current);
        int affected = manuscriptMapper.updateStatus(id, ManuscriptStatus.REJECTED.name(), rejectLevel);
        ensureSingleRowUpdated(affected, id, "reject");
        reviewRecordService.append(
                id,
                ReviewAction.REJECT,
                current.name(),
                ManuscriptStatus.REJECTED.name(),
                opinion.trim(),
                rejectLevel
        );
        log.info("manuscriptId={}, action={}, fromStatus={}, toStatus={}, rejectLevel={}",
                id, ReviewAction.REJECT.name(), current.name(), ManuscriptStatus.REJECTED.name(), rejectLevel);
        return manuscriptService.detail(id);
    }

    /**
     * 查询稿件审核流水。
     *
     * @param manuscriptId 稿件ID
     * @return 流水列表
     */
    public List<ReviewRecordVO> listRecords(Long manuscriptId) {
        manuscriptService.requireById(manuscriptId);
        return reviewRecordService.listByManuscriptId(manuscriptId);
    }

    private void ensureSingleRowUpdated(int affectedRows, Long manuscriptId, String action) {
        if (affectedRows != 1) {
            log.error("manuscriptId={}, action={}, affectedRows={}, code={}",
                    manuscriptId, action, affectedRows, ErrorCode.INTERNAL_ERROR.getCode());
            throw new BizException(ErrorCode.INTERNAL_ERROR, "更新稿件失败");
        }
    }
}
