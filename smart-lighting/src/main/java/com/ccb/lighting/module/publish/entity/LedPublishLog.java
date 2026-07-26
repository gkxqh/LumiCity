package com.ccb.lighting.module.publish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LED 节目发布记录实体 LedPublishLog
 *
 * <p>记录每次节目发布操作，用于追溯发布历史和在演示页面验证发布效果。</p>
 *
 * <p>每次 publish 操作写入一条记录，包含：
 * - 节目基本信息（冗余，避免 join）
 * - 操作人信息（通过 SecurityContext 取当前登录用户）
 * - 时间戳 + 推送状态（模拟状态，实际环境应反馈设备 ACK）</p>
 *
 * <p>表结构参考：
 * CREATE TABLE led_publish_log (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   program_id BIGINT NOT NULL COMMENT '节目ID',
 *   program_name VARCHAR(100) NOT NULL COMMENT '节目名称',
 *   media_type VARCHAR(20) NOT NULL COMMENT '媒体类型',
 *   content_preview VARCHAR(200) COMMENT '内容预览',
 *   operator VARCHAR(50) COMMENT '操作人',
 *   operator_id BIGINT COMMENT '操作人用户ID',
 *   publish_time DATETIME NOT NULL COMMENT '发布时间',
 *   push_status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '推送状态',
 *   push_message VARCHAR(500) COMMENT '推送结果描述',
 *   ... (BaseEntity 字段)
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("led_publish_log")
public class LedPublishLog extends BaseEntity implements Serializable {

    /** 节目ID：关联 led_program.id */
    private Long programId;

    /** 节目名称：冗余存储，避免查询时需要 join */
    private String programName;

    /** 媒体类型：TEXT/IMAGE/VIDEO，与 led_program 一致 */
    private String mediaType;

    /** 内容预览：文本截取前 200 字，图片/视频存文件名 */
    private String contentPreview;

    /** 操作人用户名：来自 SecurityContext 当前登录用户 */
    private String operator;

    /** 操作人用户ID：来自 SecurityContext 当前登录用户 */
    private Long operatorId;

    /** 发布时间：操作时间，与 led_program.publish_time 一致 */
    private LocalDateTime publishTime;

    /** 推送状态：SUCCESS 成功 / FAIL 失败（模拟场景固定 SUCCESS） */
    private String pushStatus;

    /** 推送结果描述：固定 "节目已推送到 LED 屏幕" */
    private String pushMessage;
}
