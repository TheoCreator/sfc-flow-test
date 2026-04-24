package com.sfc.flowtest.review.controller;

import com.sfc.flowtest.common.response.ApiResponse;
import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;
import com.sfc.flowtest.review.dto.RejectRequest;
import com.sfc.flowtest.review.service.ReviewService;
import com.sfc.flowtest.review.vo.ReviewRecordVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审核模块接口：
 * 负责通过、退回和审核流水查询。
 */
@RestController
@RequestMapping("/api/v1/manuscripts")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 审核通过，推动稿件进入下一审核节点。
     *
     * @param id 稿件ID
     * @return 更新后的稿件详情
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<ManuscriptDetailVO> approve(@PathVariable Long id) {
        return ApiResponse.success(reviewService.approve(id));
    }

    /**
     * 审核退回，退回意见为必填项。
     *
     * @param id      稿件ID
     * @param request 退回请求体
     * @return 更新后的稿件详情
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<ManuscriptDetailVO> reject(@PathVariable Long id, @RequestBody @Valid RejectRequest request) {
        return ApiResponse.success(reviewService.reject(id, request.getOpinion()));
    }

    /**
     * 查询稿件审核流水。
     *
     * @param id 稿件ID
     * @return 审核记录列表
     */
    @GetMapping("/{id}/records")
    public ApiResponse<List<ReviewRecordVO>> records(@PathVariable Long id) {
        return ApiResponse.success(reviewService.listRecords(id));
    }
}
