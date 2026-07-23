package com.ccb.lighting.module.environment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 环境传感器数据实体 EnvSensorData
 *
 * <p>智慧城市灯杆挂载多种环境传感器，采集温湿度、PM2.5/PM10、噪声、光照、风速风向等
 * 环境数据，用于环境监测、健康预警、智能联动（如光照低自动开灯）。</p>
 *
 * <p>数据特点：
 * - 时序数据：按记录时间序列入库，常做趋势图、最新值查询
 * - 字段众多：一次上报包含多个传感器读数，全部存在一行便于查询
 * - 高频写入：通常每 1~5 分钟一条记录</p>
 *
 * <p>表结构参考：
 * CREATE TABLE env_sensor_data (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   pole_id BIGINT NOT NULL COMMENT '灯杆ID',
 *   temperature DECIMAL(5,2) COMMENT '温度(℃)',
 *   humidity DECIMAL(5,2) COMMENT '湿度(%)',
 *   pm25 DECIMAL(6,2) COMMENT 'PM2.5(μg/m³)',
 *   pm10 DECIMAL(6,2) COMMENT 'PM10(μg/m³)',
 *   noise DECIMAL(6,2) COMMENT '噪声(dB)',
 *   illumination DECIMAL(8,2) COMMENT '光照(lux)',
 *   wind_speed DECIMAL(5,2) COMMENT '风速(m/s)',
 *   wind_direction VARCHAR(10) COMMENT '风向(如 N/NE/E)',
 *   record_time DATETIME NOT NULL COMMENT '记录时间',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("env_sensor_data")
public class EnvSensorData extends BaseEntity implements Serializable {

    /** 灯杆ID：关联 dev_pole.id，标识数据采集自哪个灯杆 */
    @NotNull(message = "灯杆ID不能为空")
    private Long poleId;

    /** 温度（℃）：环境温度，常温 0~40，极端 -20~50 */
    private BigDecimal temperature;

    /** 湿度（%）：空气湿度，0~100，过高需关注凝露风险 */
    private BigDecimal humidity;

    /** PM2.5（μg/m³）：细颗粒物浓度，>75 表示空气污染，>150 为重度污染 */
    private BigDecimal pm25;

    /** PM10（μg/m³）：可吸入颗粒物浓度，影响空气质量等级 */
    private BigDecimal pm10;

    /** 噪声（dB）：环境噪声，城市夜间应低于 50dB */
    private BigDecimal noise;

    /** 光照（lux）：环境光照度，可用于感光策略联动（光照低于阈值自动开灯） */
    private BigDecimal illumination;

    /** 风速（m/s）：风速，影响高空设备安全评估 */
    private BigDecimal windSpeed;

    /** 风向：如 N 北、NE 东北、E 东，用字符串存储便于扩展 */
    private String windDirection;

    /** 记录时间：数据采集时间点，时序数据的关键字段 */
    @NotNull(message = "记录时间不能为空")
    private LocalDateTime recordTime;
}
