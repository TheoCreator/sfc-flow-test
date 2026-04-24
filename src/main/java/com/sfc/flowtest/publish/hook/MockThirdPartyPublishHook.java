package com.sfc.flowtest.publish.hook;

import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 模拟第三方发布实现：
 * 当前仅记录日志，后续可替换为真实 SDK/API 调用。
 */
@Component
public class MockThirdPartyPublishHook implements ThirdPartyPublishHook {
    private static final Logger log = LoggerFactory.getLogger(MockThirdPartyPublishHook.class);

    private final String endpoint;

    public MockThirdPartyPublishHook(@Value("${sfc.publish.mock-endpoint:mock://external-platform/manuscript}") String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String name() {
        return "mock-third-party";
    }

    @Override
    public void publish(ManuscriptDetailVO manuscript) {
        log.info("hook={}, endpoint={}, manuscriptId={}, status={}, title={}",
                name(), endpoint, manuscript.getId(), manuscript.getStatus(), manuscript.getTitle());
    }
}
