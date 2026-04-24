package com.sfc.flowtest.publish.hook;

import com.sfc.flowtest.manuscript.vo.ManuscriptDetailVO;

/**
 * 第三方发布 Hook：
 * 用于对接外部平台，默认可接入多个实现。
 */
public interface ThirdPartyPublishHook {

    /**
     * Hook 名称（用于日志与流水标记）。
     *
     * @return 名称
     */
    String name();

    /**
     * 发布稿件到外部平台。
     *
     * @param manuscript 完成态稿件
     */
    void publish(ManuscriptDetailVO manuscript);
}
