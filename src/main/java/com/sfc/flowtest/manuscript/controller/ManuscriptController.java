package com.sfc.flowtest.manuscript.controller;

import com.sfc.flowtest.common.response.ApiResponse;
import com.sfc.flowtest.manuscript.dto.CreateManuscriptRequest;
import com.sfc.flowtest.manuscript.dto.SaveDraftRequest;
import com.sfc.flowtest.manuscript.service.ManuscriptService;
import com.sfc.flowtest.manuscript.vo.IdVO;
import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;
import com.sfc.flowtest.manuscript.vo.ManuscriptListTotalVO;
import com.sfc.flowtest.manuscript.vo.ManuscriptSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 稿件模块接口：
 * 负责创建、查询、保存草稿、提交审核和退回后重提。
 */
@Validated
@RestController
@RequestMapping("/api/v1/manuscripts")
public class ManuscriptController {
    private final ManuscriptService manuscriptService;

    public ManuscriptController(ManuscriptService manuscriptService) {
        this.manuscriptService = manuscriptService;
    }

    /**
     * 创建稿件，初始状态为 DRAFT。
     *
     * @param request 标题与正文
     * @return 新建稿件ID
     */
    @PostMapping
    public ApiResponse<IdVO> create(@RequestBody @Valid CreateManuscriptRequest request) {
        return ApiResponse.success(new IdVO(manuscriptService.create(request)));
    }

    /**
     * 查询稿件列表，支持按状态筛选和分页参数。
     *
     * @param status   可选状态过滤
     * @param statuses 可选多状态过滤（逗号分隔）
     * @param keyword  可选标题/正文检索关键词
     * @param pageNo   页码（从1开始，可选）
     * @param pageSize 每页条数（可选）
     * @return 稿件摘要列表
     */
    @GetMapping
    public ApiResponse<List<ManuscriptSummaryVO>> list(@RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String statuses,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer pageNo,
                                                       @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.success(manuscriptService.list(status, statuses, keyword, pageNo, pageSize));
    }

    /**
     * 统计稿件列表符合条件的总条数（与列表接口相同的 status/statuses/keyword 语义，不含分页）。
     *
     * @param status   可选单状态
     * @param statuses 可选多状态（逗号分隔）
     * @param keyword  可选关键词
     * @return total
     */
    @GetMapping("/count")
    public ApiResponse<ManuscriptListTotalVO> count(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String statuses,
                                                    @RequestParam(required = false) String keyword) {
        return ApiResponse.success(manuscriptService.count(status, statuses, keyword));
    }

    /**
     * 查询稿件详情。
     *
     * @param id 稿件ID
     * @return 稿件完整信息
     */
    @GetMapping("/{id}")
    public ApiResponse<ManuscriptDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(manuscriptService.detail(id));
    }

    /**
     * 保存草稿，仅允许 DRAFT/REJECTED 状态。
     *
     * @param id      稿件ID
     * @param request 标题与正文
     * @return 更新后的稿件详情
     */
    @PutMapping("/{id}/draft")
    public ApiResponse<ManuscriptDetailVO> saveDraft(@PathVariable Long id, @RequestBody @Valid SaveDraftRequest request) {
        return ApiResponse.success(manuscriptService.saveDraft(id, request));
    }

    /**
     * 提交审核，仅允许 DRAFT -> PENDING_1ST。
     *
     * @param id 稿件ID
     * @return 更新后的稿件详情
     */
    @PostMapping("/{id}/submit")
    public ApiResponse<ManuscriptDetailVO> submit(@PathVariable Long id) {
        return ApiResponse.success(manuscriptService.submit(id));
    }

    /**
     * 退回后重提，仅允许 REJECTED 状态。
     *
     * @param id 稿件ID
     * @return 更新后的稿件详情
     */
    @PostMapping("/{id}/resubmit")
    public ApiResponse<ManuscriptDetailVO> resubmit(@PathVariable Long id) {
        return ApiResponse.success(manuscriptService.resubmit(id));
    }
}
