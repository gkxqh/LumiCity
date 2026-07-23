package com.ccb.lighting.module.lighting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * 照明策略实体 LightStrategy
 *
 * <p>智慧城市照明系统的"大脑"——控制灯杆亮灭与亮度的策略规则。
 * 一条策略可绑定单个灯杆（poleId 非空），也可绑定灯杆群组（poleId 为空时表示全局策略）。</p>
 *
 * <p>策略分三类（strategyType）：
 * - TIME 定时策略：到 startTime 开灯、到 endTime 关灯，最常用
 * - LIGHT 感光策略：根据环境光照度自动开关（联动 env_sensor_data 表）
 * - TRAFFIC 车流策略：根据车流量动态调光（车少变暗、车多变亮，节能）</p>
 *
 * <p>继承 BaseEntity 自动拥有 id、createTime、updateTime、createBy、updateBy、deleted 字段。</p>
 *
 * <p>表结构参考：
 * CREATE TABLE light_strategy (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   strategy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
 *   strategy_type VARCHAR(20) NOT NULL COMMENT '类型：TIME定时/LIGHT感光/TRAFFIC车流',
 *   pole_id BIGINT COMMENT '灯杆ID（空表示群组策略）',
 *   brightness INT DEFAULT 100 COMMENT '亮度0-100',
 *   start_time TIME COMMENT '开始时间',
 *   end_time TIME COMMENT '结束时间',
 *   week_days VARCHAR(20) COMMENT '周几（1-7逗号分隔）',
 *   enabled TINYINT DEFAULT 1 COMMENT '是否启用：0禁用 1启用',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("light_strategy")
public class LightStrategy extends BaseEntity implements Serializable {

    /** 策略名称：便于运维人员识别（如 "人民路夜间定时"），前端必填 */
    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    /**
     * 策略类型：
     * - TIME：定时策略（按 startTime/endTime 时间段控制）
     * - LIGHT：感光策略（依据环境光照度自动调节）
     * - TRAFFIC：车流策略（依据车流量动态调节亮度，节能场景）
     */
    @NotBlank(message = "策略类型不能为空")
    private String strategyType;

    /** 灯杆ID：关联 dev_pole.id。为空表示该策略作用于全部灯杆（群组策略） */
    private Long poleId;

    /** 亮度值：0~100，0 表示关灯、100 表示最大亮度。定时策略通常夜间设为 80% 兼顾节能与照明 */
    @NotNull(message = "亮度值不能为空")
    private Integer brightness;

    /** 开始时间：定时策略的开灯时间（如 18:00:00） */
    private LocalTime startTime;

    /** 结束时间：定时策略的关灯时间（如 06:00:00，跨天则结束时间小于开始时间） */
    private LocalTime endTime;

    /** 周几生效：逗号分隔（如 "1,2,3,4,5" 表示工作日生效，"1,2,3,4,5,6,7" 表示全天生效） */
    private String weekDays;

    /** 是否启用：0=禁用，1=启用。禁用的策略不参与调度，但保留配置便于恢复 */
    private Integer enabled;
}
