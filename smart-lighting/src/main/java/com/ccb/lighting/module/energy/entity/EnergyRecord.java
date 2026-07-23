package com.ccb.lighting.module.energy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 能耗记录实体 EnergyRecord
 *
 * <p>智慧城市照明系统会持续采集每个设备的用电数据：电压、电流、功率、累计用电量。
 * 这些数据按时间序列入库，用于绘制能耗趋势图、统计电费、识别异常用电。</p>
 *
 * <p>数据特点：
 * - 时序数据：记录时间 recordTime 是关键查询字段，常做范围查询、聚合统计
 * - 关联设备：通过 deviceId 关联 dev_device 表，poleId 冗余便于按灯杆聚合
 * - 高频写入：每个设备每分钟可能产生一条记录，需考虑分区表或归档策略</p>
 *
 * <p>表结构参考：
 * CREATE TABLE energy_record (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   device_id VARCHAR(50) NOT NULL COMMENT '设备ID',
 *   pole_id BIGINT COMMENT '灯杆ID',
 *   record_time DATETIME NOT NULL COMMENT '记录时间',
 *   voltage DECIMAL(10,2) COMMENT '电压(V)',
 *   current DECIMAL(10,2) COMMENT '电流(A)',
 *   power DECIMAL(10,2) COMMENT '功率(W)',
 *   consumption DECIMAL(10,3) COMMENT '用电量(kWh)',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("energy_record")
public class EnergyRecord extends BaseEntity implements Serializable {

    /** 设备ID：关联 dev_device.device_code，标识是哪个设备的能耗数据 */
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /** 灯杆ID：冗余字段，便于按灯杆聚合统计（避免每次 join 设备表） */
    private Long poleId;

    /** 记录时间：数据采集的时间点，时序数据的关键字段 */
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;

    /** 电压（V）：单相 220V 左右为正常，过高过低都需告警 */
    private BigDecimal voltage;

    /** 电流（A）：与负载（灯泡功率）相关，异常波动可能预示设备故障 */
    private BigDecimal current;

    /** 功率（W）：瞬时功率，等于电压×电流×功率因数 */
    private BigDecimal power;

    /** 用电量（kWh）：从上次记录到本次记录的累计用电量，电费计算依据 */
    private BigDecimal consumption;
}
