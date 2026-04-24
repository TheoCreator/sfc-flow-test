package com.sfc.flowtest.manuscript.controller;

import com.sfc.flowtest.common.response.ApiResponse;
import com.sfc.flowtest.manuscript.service.ManuscriptAssetService;
import com.sfc.flowtest.manuscript.vo.AssetUploadVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 稿件富文本素材：上传与读取。
 */
@Validated
@RestController
@RequestMapping("/api/v1/manuscripts")
public class ManuscriptAssetController {
    private final ManuscriptAssetService manuscriptAssetService;

    public ManuscriptAssetController(ManuscriptAssetService manuscriptAssetService) {
        this.manuscriptAssetService = manuscriptAssetService;
    }

    /**
     * 上传图片或视频文件，返回可写入 HTML 正文的 URL。
     *
     * @param id   稿件 ID
     * @param file 表单字段名 {@code file}
     */
    @PostMapping("/{id}/assets")
    public ApiResponse<AssetUploadVO> upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(manuscriptAssetService.upload(id, file));
    }

    /**
     * 按素材 ID 读取二进制内容（供正文 img/video src 引用）。
     */
    @GetMapping("/assets/{assetId}")
    public ResponseEntity<Resource> serve(@PathVariable Long assetId) {
        return manuscriptAssetService.serve(assetId);
    }
}
