package com.sfc.flowtest.publish.event;

/**
 * 稿件完成事件：
 * 用于在三审完成后触发发布集成 Hook。
 *
 * @param manuscriptId 稿件ID
 */
public record ManuscriptCompletedEvent(Long manuscriptId) {
}
