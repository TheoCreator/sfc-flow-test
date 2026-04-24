package com.sfc.flowtest.publish.service;

import com.sfc.flowtest.common.enums.ManuscriptStatus;
import com.sfc.flowtest.common.enums.ReviewAction;
import com.sfc.flowtest.manuscript.service.ManuscriptService;
import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;
import com.sfc.flowtest.publish.event.ManuscriptCompletedEvent;
import com.sfc.flowtest.publish.hook.ThirdPartyPublishHook;
import com.sfc.flowtest.review.service.ReviewRecordService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 发布 Hook 分发器：
 * 在三审完成事务提交后触发，避免外部发布失败影响主交易。
 */
@Component
public class PublishHookDispatcher {
    private static final Logger log = LoggerFactory.getLogger(PublishHookDispatcher.class);

    private final ManuscriptService manuscriptService;
    private final ReviewRecordService reviewRecordService;
    private final List<ThirdPartyPublishHook> hooks;
    private final boolean publishEnabled;

    public PublishHookDispatcher(ManuscriptService manuscriptService,
                                 ReviewRecordService reviewRecordService,
                                 List<ThirdPartyPublishHook> hooks,
                                 @Value("${sfc.publish.enabled:true}") boolean publishEnabled) {
        this.manuscriptService = manuscriptService;
        this.reviewRecordService = reviewRecordService;
        this.hooks = hooks;
        this.publishEnabled = publishEnabled;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onManuscriptCompleted(ManuscriptCompletedEvent event) {
        if (!publishEnabled) {
            log.info("publish hook disabled, manuscriptId={}", event.manuscriptId());
            return;
        }
        if (hooks.isEmpty()) {
            log.warn("no publish hooks registered, manuscriptId={}", event.manuscriptId());
            return;
        }
        ManuscriptDetailVO manuscript = manuscriptService.detail(event.manuscriptId());
        for (ThirdPartyPublishHook hook : hooks) {
            try {
                hook.publish(manuscript);
                reviewRecordService.append(
                        manuscript.getId(),
                        ReviewAction.PUBLISH,
                        ManuscriptStatus.COMPLETED.name(),
                        ManuscriptStatus.COMPLETED.name(),
                        "hook=" + hook.name() + " 发布成功",
                        null
                );
            } catch (Exception ex) {
                log.error("publish hook failed, hook={}, manuscriptId={}", hook.name(), event.manuscriptId(), ex);
                reviewRecordService.append(
                        manuscript.getId(),
                        ReviewAction.PUBLISH,
                        ManuscriptStatus.COMPLETED.name(),
                        ManuscriptStatus.COMPLETED.name(),
                        "hook=" + hook.name() + " 发布失败: " + ex.getMessage(),
                        null
                );
            }
        }
    }
}
