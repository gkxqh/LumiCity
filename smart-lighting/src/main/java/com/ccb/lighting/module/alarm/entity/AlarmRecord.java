package com.ccb.lighting.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警记录实体 AlarmRecord
 *
 * <p>智慧城市照明系统的"哨兵"——设备发生异常时自动生成告警记录，
 * 运维人员在系统里处理告警，形成"产生 → 处理 → 闭环"的闭环流程。</p>
 *
 * <p>告警类型（alarmType）：
 * - OFFLINE：离线告警（设备超过阈值未上报心跳）
 * - OVERVOLTAGE：过压告警（电压超过安全范围）
 * - OVERCURRENT：过流告警（电流超过额定值）
 * - ABNORMAL：其他异常（如灯泡损坏、传感器故障）</p>
 *
 * <p>告警级别（alarmLevel）：
 * - 1 严重：影响公共安全（如整条路灯全灭），需立即处理
 * - 2 重要：影响业务功能（如单灯故障），需当天处理
 * - 3 一般：潜在风险（如电压偏高），需关注</p>
 *
 * <p>处理状态（status）：
 * - 0 未处理：刚产生，等待运维介入
 * - 1 处理中：运维已接手，正在排查/修复
 * - 2 已闭环：处理完成并验证通过</p>
 *
 * <p>表结构参考：
 * CREATE TABLE alarm_record (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   device_id VARCHAR(50) NOT NULL COMMENT '设备ID',
 *   pole_id BIGINT COMMENT '灯杆ID',
 *   alarm_type VARCHAR(20) NOT NULL COMMENT '告警类型',
 *   alarm_level TINYINT DEFAULT 3 COMMENT '级别：1严重 2重要 3一般',
 *   alarm_content VARCHAR(500) COMMENT '告警内容',
 *   alarm_time DATETIME NOT NULL COMMENT '告警时间',
 *   status TINYINT DEFAULT 0 COMMENT '状态：0未处理 1处理中 2已闭环',
 *   handle_time DATETIME COMMENT '处理时间',
 *   handle_user VARCHAR(50) COMMENT '处理人',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alarm_record")
public class AlarmRecord extends BaseEntity implements Serializable {

    /** 设备ID：关联 dev_device.device_code，标识告警的设备 */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /** 灯杆ID：冗余字段，便于按灯杆聚合统计告警 */
    private Long poleId;

    /**
     * 告警类型：
     * - OFFLINE：离线
     * - OVERVOLTAGE：过压
     * - OVERCURRENT：过流
     * - ABNORMAL：异常
     */
    @NotBlank(message = "告警类型不能为空")
    private String alarmType;

    /** 告警级别：1=严重，2=重要，3=一般。默认为 3 一般 */
    @NotNull(message = "告警级别不能为空")
    private Integer alarmLevel;

    /** 告警内容：人类可读的告警描述（如 "电压 250V 超出阈值 220V"） */
    private String alarmContent;

    /** 告警时间：告警产生的时间点（设备上报时间或系统检测时间） */
    @NotNull(message = "告警时间不能为空")
    private LocalDateTime alarmTime;

    /** 状态：0=未处理，1=处理中，2=已闭环。默认为 0 未处理 */
    private Integer status;

    /** 处理时间：运维人员闭环告警时的时间戳 */
    private LocalDateTime handleTime;

    /** 处理人：闭环告警的运维人员用户名 */
    private String handleUser;
}
