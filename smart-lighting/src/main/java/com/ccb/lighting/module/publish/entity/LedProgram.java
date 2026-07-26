package com.ccb.lighting.module.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LED 节目实体 LedProgram
 *
 * <p>灯杆挂载 LED 屏，用于发布公共信息、应急通知、广告等。
 * 本表管理 LED 屏要播放的节目内容、播放模式、发布状态。</p>
 *
 * <p>节目生命周期：创建（待发布）→ 发布（已发布）→ 下线（已下线）</p>
 *
 * <p>表结构参考：
 * CREATE TABLE led_program (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   program_name VARCHAR(100) NOT NULL COMMENT '节目名称',
 *   content TEXT COMMENT '节目内容',
 *   media_type VARCHAR(20) NOT NULL COMMENT '媒体类型：TEXT/IMAGE/VIDEO',
 *   screen_id BIGINT COMMENT '屏幕ID',
 *   play_mode VARCHAR(20) DEFAULT 'LOOP' COMMENT '播放模式：LOOP循环/ONCE单次',
 *   start_time DATETIME COMMENT '开始时间',
 *   end_time DATETIME COMMENT '结束时间',
 *   status TINYINT DEFAULT 0 COMMENT '状态：0待发布 1已发布 2已下线',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("led_program")
public class LedProgram extends BaseEntity implements Serializable {

    /** 节目名称：便于运维识别管理（如 "周一限行通知"） */
    @NotBlank(message = "节目名称不能为空")
    private String programName;

    /** 节目内容：TEXT 类型存文本；IMAGE/VIDEO 类型存资源 URL 或文件路径 */
    private String content;

    /**
     * 媒体类型：
     * - TEXT：文本（直接展示文字）
     * - IMAGE：图片（展示静态图）
     * - VIDEO：视频（循环播放视频文件）
     */
    @NotBlank(message = "媒体类型不能为空")
    private String mediaType;

    /** 屏幕ID：关联 LED 屏设备，标识节目发布到哪块屏 */
    private Long screenId;

    /**
     * 播放模式：
     * - LOOP：循环播放（默认）
     * - ONCE：单次播放（播放完停止）
     */
    private String playMode;

    /** 开始时间：节目生效开始时间，到期后自动停止播放 */
    private LocalDateTime startTime;

    /** 结束时间：节目失效时间，到点自动下线 */
    private LocalDateTime endTime;

    /** 最近发布时间：最后一次 publish 操作的时间，用于展示发布记录 */
    private LocalDateTime publishTime;

    /** 关联的设备名称（LedProgramMapper XML 中 left join dev_device 带回，非数据库字段） */
    @TableField(exist = false)
    private String screenName;

    /**
     * 状态：
     * - 0 待发布：刚创建，未推送至屏幕
     * - 1 已发布：已推送至屏幕，正在播放
     * - 2 已下线：手动下线或到期自动下线
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
