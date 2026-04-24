package com.sfc.flowtest.manuscript.service;

import com.sfc.flowtest.common.enums.ErrorCode;
import com.sfc.flowtest.common.enums.ManuscriptStatus;
import com.sfc.flowtest.common.exception.BizException;
import com.sfc.flowtest.config.StorageProperties;
import com.sfc.flowtest.manuscript.entity.MsManuscript;
import com.sfc.flowtest.manuscript.entity.MsManuscriptAsset;
import com.sfc.flowtest.manuscript.mapper.MsManuscriptAssetMapper;
import com.sfc.flowtest.manuscript.vo.AssetUploadVO;
import com.sfc.flowtest.review.service.StateMachineService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 稿件素材上传与读取。
 */
@Service
public class ManuscriptAssetService {
    private static final Logger log = LoggerFactory.getLogger(ManuscriptAssetService.class);

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "video/mp4",
            "video/webm",
            "video/quicktime"
    );

    private static final Map<String, String> MIME_TO_EXT = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp"),
            Map.entry("video/mp4", "mp4"),
            Map.entry("video/webm", "webm"),
            Map.entry("video/quicktime", "mov")
    );

    private final StorageProperties storageProperties;
    private final MsManuscriptAssetMapper assetMapper;
    private final ManuscriptService manuscriptService;
    private final StateMachineService stateMachineService;

    public ManuscriptAssetService(StorageProperties storageProperties,
                                  MsManuscriptAssetMapper assetMapper,
                                  ManuscriptService manuscriptService,
                                  StateMachineService stateMachineService) {
        this.storageProperties = storageProperties;
        this.assetMapper = assetMapper;
        this.manuscriptService = manuscriptService;
        this.stateMachineService = stateMachineService;
    }

    /**
     * 上传素材：仅稿件处于 DRAFT/REJECTED 时允许。
     *
     * @param manuscriptId 稿件 ID
     * @param file         上传文件
     * @return 可嵌入正文的访问地址
     */
    @Transactional
    public AssetUploadVO upload(Long manuscriptId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请选择要上传的文件");
        }
        MsManuscript manuscript = manuscriptService.requireById(manuscriptId);
        ManuscriptStatus current = stateMachineService.parseStatus(manuscript.getStatus());
        if (!(current == ManuscriptStatus.DRAFT || current == ManuscriptStatus.REJECTED)) {
            throw new BizException(ErrorCode.ILLEGAL_STATE, "当前状态不允许上传素材: " + current.name());
        }
        long max = storageProperties.getMaxAssetBytes();
        if (file.getSize() > max) {
            throw new BizException(ErrorCode.ASSET_TOO_LARGE, "素材大小不能超过 " + (max / (1024 * 1024)) + "MB");
        }
        String mime = normalizeMime(file.getContentType());
        if (!StringUtils.hasText(mime) || !ALLOWED_TYPES.contains(mime)) {
            throw new BizException(ErrorCode.ASSET_INVALID_TYPE, "仅支持常见图片（JPEG/PNG/GIF/WebP）与视频（MP4/WebM/MOV）");
        }
        String ext = MIME_TO_EXT.get(mime);
        String objectKey = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path manuscriptDir = storageProperties.resolvedRoot().resolve(String.valueOf(manuscriptId));
        Path target = manuscriptDir.resolve(objectKey);
        try {
            Files.createDirectories(manuscriptDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("manuscriptId={}, save asset failed", manuscriptId, ex);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // ignore cleanup failure
            }
            throw new BizException(ErrorCode.INTERNAL_ERROR, "素材保存失败");
        }

        MsManuscriptAsset row = new MsManuscriptAsset();
        row.setManuscriptId(manuscriptId);
        row.setStorageObjectKey(objectKey);
        row.setOriginalFilename(safeOriginalName(file.getOriginalFilename()));
        row.setContentType(mime);
        row.setSizeBytes(file.getSize());
        int inserted = assetMapper.insert(row);
        if (inserted != 1 || row.getId() == null) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // ignore
            }
            throw new BizException(ErrorCode.INTERNAL_ERROR, "素材元数据写入失败");
        }

        String url = "/api/v1/manuscripts/assets/" + row.getId();
        return new AssetUploadVO(row.getId(), url);
    }

    /**
     * 读取已存储素材二进制流。
     *
     * @param assetId 素材 ID
     * @return 文件响应
     */
    public ResponseEntity<Resource> serve(Long assetId) {
        MsManuscriptAsset asset = assetMapper.selectById(assetId);
        if (asset == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        Path root = storageProperties.resolvedRoot();
        Path manuscriptDir = root.resolve(String.valueOf(asset.getManuscriptId())).normalize();
        Path file = manuscriptDir.resolve(asset.getStorageObjectKey()).normalize();
        if (!file.startsWith(manuscriptDir) || !Files.isRegularFile(file)) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        }
        FileSystemResource resource = new FileSystemResource(file.toFile());
        MediaType mediaType = MediaType.parseMediaType(asset.getContentType());
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(asset.getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private static String normalizeMime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String v = raw.trim();
        int semi = v.indexOf(';');
        if (semi >= 0) {
            v = v.substring(0, semi).trim();
        }
        return v.toLowerCase(Locale.ROOT);
    }

    private static String safeOriginalName(String name) {
        if (!StringUtils.hasText(name)) {
            return "file";
        }
        String cleaned = name.replace("\\", "_").replace("/", "_").trim();
        if (cleaned.length() > 200) {
            return cleaned.substring(0, 200);
        }
        return cleaned;
    }
}
