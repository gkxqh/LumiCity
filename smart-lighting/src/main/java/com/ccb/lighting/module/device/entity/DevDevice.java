package com.ccb.lighting.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备实体 DevDevice
 *
 * <p>智慧城市照明系统中，设备是挂载在灯杆上的各类终端：照明灯、摄像头、传感器、LED屏、广播。
 * 一个灯杆可挂载多个设备（一对多关系，通过 poleId 关联）。</p>
 *
 * <p>继承 BaseEntity 自动拥有 id、createTime、updateTime、createBy、updateBy、deleted 字段，
 * 这里只声明设备特有的业务字段。</p>
 *
 * <p>注解说明：
 * - @TableName("dev_device")：指定数据库表名
 * - @EqualsAndHashCode(callSuper = true)：继承场景必须加，让 equals/hashCode 包含父类字段
 * - @NotBlank：Jakarta Bean Validation 注解，配合 Controller 的 @Valid 做参数校验</p>
 *
 * <p>表结构参考（学习时建表用）：
 * CREATE TABLE dev_device (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   device_code VARCHAR(50) NOT NULL COMMENT '设备编号（唯一）',
 *   device_name VARCHAR(100) NOT NULL COMMENT '设备名称',
 *   device_type VARCHAR(20) NOT NULL COMMENT '设备类型：LIGHT/CAMERA/SENSOR/LED_SCREEN/BROADCAST',
 *   pole_id BIGINT COMMENT '所属灯杆ID',
 *   model VARCHAR(100) COMMENT '设备型号',
 *   vendor VARCHAR(100) COMMENT '厂商',
 *   status TINYINT DEFAULT 0 COMMENT '状态：0离线 1在线 2故障',
 *   last_online_time DATETIME COMMENT '最后在线时间',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dev_device")
public class DevDevice extends BaseEntity implements Serializable {

    /** 设备编号：业务唯一编码（如 "L-2024-0001" 表示照明灯），新增时需查重，前端必填 */
    @NotBlank(message = "设备编号不能为空")
    private String deviceCode;

    /** 设备名称：用于前端展示（如 "人民路1号路灯"），前端必填 */
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    /**
     * 设备类型：枚举值，用字符串存储便于扩展
     * - LIGHT：照明灯（核心设备，控制开关/亮度）
     * - CAMERA：摄像头（安防监控）
     * - SENSOR：传感器（温湿度/PM2.5/车流量等环境监测）
     * - LED_SCREEN：LED显示屏（信息发布、广告）
     * - BROADCAST：广播（应急通知、音乐播放）
     */
    private String deviceType;

    /** 所属灯杆ID：关联 dev_pole.id，表示设备挂载在哪个灯杆上。新增设备时校验灯杆是否存在 */
    private Long poleId;

    /** 设备型号：厂商定义的产品型号，便于备件替换、故障匹配 */
    private String model;

    /** 厂商：设备生产厂家，便于售后联系、质保追溯 */
    private String vendor;

    /** 状态：0=离线，1=在线，2=故障。由设备心跳上报或定时巡检更新 */
    private Integer status;

    /** 最后在线时间：设备最近一次心跳上报时间，用于判断是否离线（超过阈值未上报则置离线） */
    private LocalDateTime lastOnlineTime;
}
