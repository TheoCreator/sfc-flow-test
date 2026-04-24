-- 三审制稿件工作台 MVP — 库: sfc_flow_test
-- 字符集与排序规则（与 Spring 连接串 utf8 / Asia/Shanghai 一致）
-- 执行前: CREATE DATABASE IF NOT EXISTS sfc_flow_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sfc_flow_test;

-- ---------------------------------------------------------------------------
-- 稿件主表
-- status 语义（与产品约定一致，应用层枚举对齐）:
--   DRAFT           草稿
--   PENDING_1ST     待一审
--   PENDING_2ND     待二审
--   PENDING_3RD     待三审
--   REJECTED        已退回（仅此时 reject_review_level 有值）
--   COMPLETED       已完成（归档，禁止修改）
-- reject_review_level: 被退回时「当时所处的审核关」1=一审关 2=二审关 3=三审关
--   重提规则: 回到该关的「前一关」→ level1→DRAFT, level2→PENDING_1ST, level3→PENDING_2ND
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ms_manuscript (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    title                VARCHAR(500)    NOT NULL COMMENT '标题',
    body                 MEDIUMTEXT      NOT NULL COMMENT '正文',
    status               VARCHAR(32)     NOT NULL COMMENT '当前状态枚举值',
    reject_review_level  TINYINT UNSIGNED NULL COMMENT '退回时所在审核关(1/2/3)，非REJECTED时为NULL',
    created_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_ms_manuscript_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='稿件';

-- ---------------------------------------------------------------------------
-- 审核与操作流水（通过/退回/提交/保存草稿/重新提交等统一落痕）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ms_review_record (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    manuscript_id  BIGINT UNSIGNED NOT NULL COMMENT '稿件ID',
    action         VARCHAR(32)     NOT NULL COMMENT '动作: SAVE_DRAFT,SUBMIT,APPROVE,REJECT,RESUBMIT 等',
    from_status    VARCHAR(32)     NULL COMMENT '变更前状态',
    to_status      VARCHAR(32)     NULL COMMENT '变更后状态',
    opinion        VARCHAR(2000)   NULL COMMENT '意见；REJECT 时必填',
    reject_level   TINYINT UNSIGNED NULL COMMENT '若为退回，记录当时审核关(1/2/3)，与稿件表语义一致',
    operator_id    BIGINT UNSIGNED NULL COMMENT '操作人ID，MVP可同一用户填同一值',
    operator_name  VARCHAR(64)     NULL COMMENT '操作人展示名，可选',
    created_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '记录时间',
    PRIMARY KEY (id),
    KEY idx_ms_review_manuscript_time (manuscript_id, created_at),
    CONSTRAINT fk_ms_review_manuscript
        FOREIGN KEY (manuscript_id) REFERENCES ms_manuscript (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审核与操作记录';
