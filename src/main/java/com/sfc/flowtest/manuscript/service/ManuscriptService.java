package com.sfc.flowtest.manuscript.service;

import com.sfc.flowtest.common.enums.ErrorCode;
import com.sfc.flowtest.common.enums.ManuscriptStatus;
import com.sfc.flowtest.common.enums.ReviewAction;
import com.sfc.flowtest.common.exception.BizException;
import com.sfc.flowtest.manuscript.dto.CreateManuscriptRequest;
import com.sfc.flowtest.manuscript.dto.SaveDraftRequest;
import com.sfc.flowtest.manuscript.entity.MsManuscript;
import com.sfc.flowtest.manuscript.mapper.MsManuscriptMapper;
import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;
import com.sfc.flowtest.manuscript.vo.ManuscriptListTotalVO;
import com.sfc.flowtest.manuscript.vo.ManuscriptSummaryVO;
import com.sfc.flowtest.review.service.ReviewRecordService;
import com.sfc.flowtest.review.service.StateMachineService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 稿件领域服务：
 * 负责稿件创建、保存草稿、提交审核、重提和查询编排。
 */
@Service
public class ManuscriptService {
    private static final Logger log = LoggerFactory.getLogger(ManuscriptService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final MsManuscriptMapper manuscriptMapper;
    private final ReviewRecordService reviewRecordService;
    private final StateMachineService stateMachineService;

    public ManuscriptService(MsManuscriptMapper manuscriptMapper,
                             ReviewRecordService reviewRecordService,
                             StateMachineService stateMachineService) {
        this.manuscriptMapper = manuscriptMapper;
        this.reviewRecordService = reviewRecordService;
        this.stateMachineService = stateMachineService;
    }

    /**
     * 创建稿件并落一条 SAVE_DRAFT 流水。
     *
     * @param request 创建请求
     * @return 稿件ID
     */
    @Transactional
    public Long create(CreateManuscriptRequest request) {
        MsManuscript manuscript = new MsManuscript();
        manuscript.setTitle(request.getTitle().trim());
        manuscript.setBody(request.getBody().trim());
        manuscript.setStatus(ManuscriptStatus.DRAFT.name());
        manuscript.setRejectReviewLevel(null);
        manuscriptMapper.insert(manuscript);
        reviewRecordService.append(manuscript.getId(), ReviewAction.SAVE_DRAFT, null, ManuscriptStatus.DRAFT.name(), null, null);
        log.info("manuscriptId={}, action={}, toStatus={}", manuscript.getId(), ReviewAction.SAVE_DRAFT.name(), ManuscriptStatus.DRAFT.name());
        return manuscript.getId();
    }

    /**
     * 查询稿件列表，支持状态过滤和可选分页。
     *
     * @param statusRaw   单状态过滤（可空）
     * @param statusesRaw 多状态过滤（逗号分隔，可空）
     * @param keywordRaw  标题/正文关键词（可空）
     * @param pageNo      页码（可空）
     * @param pageSize    每页数量（可空）
     * @return 稿件摘要列表
     */
    public List<ManuscriptSummaryVO> list(String statusRaw, String statusesRaw, String keywordRaw, Integer pageNo, Integer pageSize) {
        List<String> statuses = resolveStatuses(statusRaw, statusesRaw);
        String keyword = resolveKeyword(keywordRaw);
        Integer resolvedPageNo = null;
        Integer resolvedPageSize = null;
        Integer offset = null;
        if (pageNo != null || pageSize != null) {
            resolvedPageNo = pageNo == null ? 1 : pageNo;
            resolvedPageSize = pageSize == null ? 10 : pageSize;
            if (resolvedPageNo < 1 || resolvedPageSize < 1) {
                throw new BizException(ErrorCode.PARAM_INVALID, "pageNo/pageSize 必须大于0");
            }
            if (resolvedPageSize > MAX_PAGE_SIZE) {
                throw new BizException(ErrorCode.PARAM_INVALID, "pageSize 不能超过 " + MAX_PAGE_SIZE);
            }
            offset = (resolvedPageNo - 1) * resolvedPageSize;
        }
        return manuscriptMapper.selectList(statuses, keyword, resolvedPageSize, offset).stream().map(entity -> {
            ManuscriptSummaryVO vo = new ManuscriptSummaryVO();
            vo.setId(entity.getId());
            vo.setTitle(entity.getTitle());
            vo.setStatus(entity.getStatus());
            vo.setUpdatedAt(entity.getUpdatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 统计与列表相同筛选条件下的稿件总条数（不含分页）。
     *
     * @param statusRaw   单状态过滤（可空）
     * @param statusesRaw 多状态过滤（逗号分隔，可空）
     * @param keywordRaw  标题/正文关键词（可空）
     * @return 总条数
     */
    public ManuscriptListTotalVO count(String statusRaw, String statusesRaw, String keywordRaw) {
        List<String> statuses = resolveStatuses(statusRaw, statusesRaw);
        String keyword = resolveKeyword(keywordRaw);
        long total = manuscriptMapper.countList(statuses, keyword);
        return new ManuscriptListTotalVO(total);
    }

    /**
     * 查询稿件详情。
     *
     * @param id 稿件ID
     * @return 稿件详情
     */
    public ManuscriptDetailVO detail(Long id) {
        MsManuscript manuscript = requireById(id);
        return toDetailVO(manuscript);
    }

    /**
     * 保存草稿，仅允许 DRAFT/REJECTED 状态。
     *
     * @param id      稿件ID
     * @param request 保存请求
     * @return 更新后的稿件详情
     */
    @Transactional
    public ManuscriptDetailVO saveDraft(Long id, SaveDraftRequest request) {
        MsManuscript manuscript = requireById(id);
        ManuscriptStatus current = stateMachineService.parseStatus(manuscript.getStatus());
        if (!(current == ManuscriptStatus.DRAFT || current == ManuscriptStatus.REJECTED)) {
            throw new BizException(ErrorCode.ILLEGAL_STATE, "当前状态不允许保存草稿: " + current.name());
        }
        int affected = manuscriptMapper.updateDraftContent(id, request.getTitle().trim(), request.getBody().trim());
        ensureSingleRowUpdated(affected, id, "saveDraft");
        reviewRecordService.append(id, ReviewAction.SAVE_DRAFT, current.name(), current.name(), null, manuscript.getRejectReviewLevel());
        log.info("manuscriptId={}, action={}, fromStatus={}, toStatus={}", id, ReviewAction.SAVE_DRAFT.name(), current.name(), current.name());
        return detail(id);
    }

    /**
     * 提交审核，仅允许 DRAFT 状态。
     *
     * @param id 稿件ID
     * @return 更新后的稿件详情
     */
    @Transactional
    public ManuscriptDetailVO submit(Long id) {
        MsManuscript manuscript = requireById(id);
        ManuscriptStatus current = stateMachineService.parseStatus(manuscript.getStatus());
        if (current != ManuscriptStatus.DRAFT) {
            throw new BizException(ErrorCode.ILLEGAL_STATE, "当前状态不允许提交审核: " + current.name());
        }
        int affected = manuscriptMapper.updateStatus(id, ManuscriptStatus.PENDING_1ST.name(), null);
        ensureSingleRowUpdated(affected, id, "submit");
        reviewRecordService.append(id, ReviewAction.SUBMIT, current.name(), ManuscriptStatus.PENDING_1ST.name(), null, null);
        log.info("manuscriptId={}, action={}, fromStatus={}, toStatus={}", id, ReviewAction.SUBMIT.name(), current.name(), ManuscriptStatus.PENDING_1ST.name());
        return detail(id);
    }

    /**
     * 退回后重新提交，仅允许 REJECTED 状态。
     *
     * @param id 稿件ID
     * @return 更新后的稿件详情
     */
    @Transactional
    public ManuscriptDetailVO resubmit(Long id) {
        MsManuscript manuscript = requireById(id);
        ManuscriptStatus current = stateMachineService.parseStatus(manuscript.getStatus());
        if (current != ManuscriptStatus.REJECTED) {
            throw new BizException(ErrorCode.ILLEGAL_STATE, "当前状态不允许重新提交: " + current.name());
        }
        ManuscriptStatus target = stateMachineService.resubmitTarget(manuscript.getRejectReviewLevel());
        int affected = manuscriptMapper.updateStatus(id, target.name(), null);
        ensureSingleRowUpdated(affected, id, "resubmit");
        reviewRecordService.append(id, ReviewAction.RESUBMIT, current.name(), target.name(), null, manuscript.getRejectReviewLevel());
        log.info("manuscriptId={}, action={}, fromStatus={}, toStatus={}, rejectLevel={}",
                id, ReviewAction.RESUBMIT.name(), current.name(), target.name(), manuscript.getRejectReviewLevel());
        return detail(id);
    }

    /**
     * 校验稿件是否存在，不存在时抛出业务异常。
     *
     * @param id 稿件ID
     * @return 稿件实体
     */
    public MsManuscript requireById(Long id) {
        MsManuscript manuscript = manuscriptMapper.selectById(id);
        if (manuscript == null) {
            throw new BizException(ErrorCode.MANUSCRIPT_NOT_FOUND);
        }
        return manuscript;
    }

    private ManuscriptDetailVO toDetailVO(MsManuscript entity) {
        ManuscriptDetailVO vo = new ManuscriptDetailVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setBody(entity.getBody());
        vo.setStatus(entity.getStatus());
        vo.setRejectReviewLevel(entity.getRejectReviewLevel());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private void ensureSingleRowUpdated(int affectedRows, Long manuscriptId, String action) {
        if (affectedRows != 1) {
            log.error("manuscriptId={}, action={}, affectedRows={}, code={}",
                    manuscriptId, action, affectedRows, ErrorCode.INTERNAL_ERROR.getCode());
            throw new BizException(ErrorCode.INTERNAL_ERROR, "更新稿件失败");
        }
    }

    private List<String> resolveStatuses(String statusRaw, String statusesRaw) {
        List<String> merged = new ArrayList<>();
        if (StringUtils.hasText(statusRaw)) {
            merged.add(stateMachineService.parseStatus(statusRaw.trim()).name());
        }
        if (StringUtils.hasText(statusesRaw)) {
            for (String token : statusesRaw.split(",")) {
                if (!StringUtils.hasText(token)) {
                    continue;
                }
                merged.add(stateMachineService.parseStatus(token.trim()).name());
            }
        }
        return merged.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private String resolveKeyword(String keywordRaw) {
        if (!StringUtils.hasText(keywordRaw)) {
            return null;
        }
        String keyword = keywordRaw.trim();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID, "keyword 长度不能超过 " + MAX_KEYWORD_LENGTH);
        }
        return keyword;
    }
}
