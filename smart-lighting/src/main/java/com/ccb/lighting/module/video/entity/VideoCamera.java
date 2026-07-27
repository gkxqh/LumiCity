package com.ccb.lighting.module.video.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 视频摄像头实体 VideoCamera
 *
 * <p>智慧城市灯杆常挂载安防摄像头，用于城市监控、应急指挥、交通管理。
 * 本表管理摄像头的基础信息与 RTSP 流地址，前端通过流地址接入视频播放器。</p>
 *
 * <p>表结构参考：
 * CREATE TABLE video_camera (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   camera_name VARCHAR(100) NOT NULL COMMENT '摄像头名称',
 *   pole_id BIGINT COMMENT '灯杆ID',
 *   stream_url VARCHAR(500) NOT NULL COMMENT 'RTSP流地址',
 *   status TINYINT DEFAULT 0 COMMENT '状态：0离线 1在线 2故障',
 *   ptz_enable TINYINT DEFAULT 0 COMMENT '是否支持云台：0否 1是',
 *   resolution VARCHAR(20) COMMENT '分辨率（如 1080P/4K）',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("video_camera")
public class VideoCamera extends BaseEntity implements Serializable {

    /** 摄像头名称：用于前端展示（如 "人民路1号摄像头"） */
    @NotBlank(message = "摄像头名称不能为空")
    private String cameraName;

    /** 灯杆ID：关联 dev_pole.id，标识摄像头挂载在哪个灯杆上 */
    private Long poleId;

    /** RTSP 流地址：前端视频播放器通过此地址拉流（如 rtsp://admin:pass@1.2.3.4:554/stream1） */
    @NotBlank(message = "RTSP流地址不能为空")
    private String streamUrl;

    /** 状态：0=离线，1=在线，2=故障。由心跳上报或巡检更新 */
    private Integer status;

    /** 是否支持云台：0=否，1=是。支持云台的摄像头可远程控制方向、焦距 */
    private Integer ptzEnable;

    /** 分辨率：如 720P、1080P、4K。影响清晰度与带宽占用 */
    private String resolution;

    /**所属灯杆，用于前端展示摄像头挂载位置（冗余字段，非数据库字段）*/
    @TableField(exist = false)
    private String poleName;
}
