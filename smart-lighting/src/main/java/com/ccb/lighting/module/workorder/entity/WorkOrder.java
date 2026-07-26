package com.ccb.lighting.module.workorder.entity;

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
 * 工单实体 WorkOrder
 *
 * <p>智慧城市照明系统的"运维中枢"——设备故障、巡检任务都通过工单流转，
 * 形成完整闭环：创建 → 派单 → 处理 → 完成 → 验收。</p>
 *
 * <p>工单类型（orderType）：
 * - INSPECT：巡检工单（定期巡检设备，预防性维护）
 * - REPAIR：维修工单（接到告警后创建，针对性维修）</p>
 *
 * <p>工单状态（status）流转：
 * 0 待处理 → 1 处理中 → 2 已完成 → 3 已验收
 * - 0 待处理：工单创建后等待派单
 * - 1 处理中：已派单给运维人员，正在处理
 * - 2 已完成：运维人员处理完成，待验收
 * - 3 已验收：管理员验收通过，工单闭环</p>
 *
 * <p>优先级（priority）：
 * - 1 高：影响公共安全，需立即处理
 * - 2 中：影响业务，需当天处理
 * - 3 低：常规巡检，按计划处理</p>
 *
 * <p>注意：createTime 已在 BaseEntity 中定义，本类不再重复声明。
 * finishTime 为业务字段，表示工单实际完成时间。</p>
 *
 * <p>表结构参考：
 * CREATE TABLE work_order (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   order_no VARCHAR(50) NOT NULL COMMENT '工单编号（唯一）',
 *   order_type VARCHAR(20) NOT NULL COMMENT '类型：INSPECT巡检/REPAIR维修',
 *   title VARCHAR(200) NOT NULL COMMENT '标题',
 *   description VARCHAR(1000) COMMENT '描述',
 *   device_id VARCHAR(50) COMMENT '设备ID',
 *   pole_id BIGINT COMMENT '灯杆ID',
 *   assignee_id BIGINT COMMENT '指派人ID',
 *   priority TINYINT DEFAULT 2 COMMENT '优先级：1高 2中 3低',
 *   status TINYINT DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已完成 3已验收',
 *   finish_time DATETIME COMMENT '完成时间',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity implements Serializable {

    /** 工单编号：业务唯一编码（如 "WO-2026-0001"），系统自动生成，便于线下沟通 */
    @NotBlank(message = "工单编号不能为空")
    private String orderNo;

    /**
     * 工单类型：
     * - INSPECT：巡检（定期检查，预防性维护）
     * - REPAIR：维修（接到告警后处理）
     */
    @NotBlank(message = "工单类型不能为空")
    private String orderType;

    /** 工单标题：简述问题（如 "人民路1号灯杆离线"） */
    @NotBlank(message = "工单标题不能为空")
    private String title;

    /** 工单描述：详细说明问题现象、影响范围、处理建议 */
    private String description;

    /** 设备ID：关联 dev_device.device_code，标识故障设备 */
    private String deviceId;

    /** 灯杆ID：关联 dev_pole.id，便于运维人员现场定位 */
    private Long poleId;

    @TableField(exist = false)
    private String poleName;

    /** 指派人ID：关联 sys_user.id，标识工单派给谁处理 */
    private Long assigneeId;

    /** 优先级：1=高，2=中，3=低。默认 2 中 */
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    /**
     * 状态：0=待处理，1=处理中，2=已完成，3=已验收
     * 默认 0 待处理，由派单动作推进到 1 处理中
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 完成时间：运维人员点"完成"时的时间戳，用于统计处理时长 */
    private LocalDateTime finishTime;

    /** 告警ID：标识关联告警 */
    private Long alarmId;
}
