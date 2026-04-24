package com.sfc.flowtest.manuscript.vo;

/**
 * 素材上传成功后的返回体。
 * <p>{@code location} 字段与 TinyMCE 官方示例对齐。</p>
 */
public class AssetUploadVO {
    private final long id;
    private final String url;
    private final String location;

    public AssetUploadVO(long id, String url) {
        this.id = id;
        this.url = url;
        this.location = url;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getLocation() {
        return location;
    }
}
