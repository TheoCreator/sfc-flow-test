package com.sfc.flowtest.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 稿件素材本地存储配置。
 */
@Component
@ConfigurationProperties(prefix = "sfc.storage")
public class StorageProperties {
    /**
     * 素材文件根目录（绝对路径或用户目录下相对路径由应用启动时解析）。
     */
    private String root = Paths.get(System.getProperty("user.home"), ".sfc-flow-test", "storage").toString();

    /**
     * 单个素材最大字节数。
     */
    private long maxAssetBytes = 50L * 1024 * 1024;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public long getMaxAssetBytes() {
        return maxAssetBytes;
    }

    public void setMaxAssetBytes(long maxAssetBytes) {
        this.maxAssetBytes = maxAssetBytes;
    }

    public Path resolvedRoot() {
        return Paths.get(root).toAbsolutePath().normalize();
    }
}
