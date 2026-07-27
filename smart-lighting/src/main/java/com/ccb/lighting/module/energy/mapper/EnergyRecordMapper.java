package com.ccb.lighting.module.energy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 能耗记录 Mapper 接口
 *
 * <p>继承 BaseMapper<EnergyRecord> 自动拥有单表 CRUD。
 * 趋势查询、统计汇总均基于 BaseMapper 的 selectList + 条件构造器实现。</p>
 */
@Mapper
public interface EnergyRecordMapper extends BaseMapper<EnergyRecord> {

    /**
     * 统计汇总
     *
     * <p>MyBatis 注解 @Select 写原生 SQL。</p>
     */
    @Select({
            "SELECT",
            "  COALESCE(SUM(CASE WHEN DATE(record_time) = CURDATE() THEN consumption ELSE 0 END), 0) AS today,",
            "  COALESCE(SUM(CASE WHEN DATE(record_time) = CURDATE() - INTERVAL 1 DAY THEN consumption ELSE 0 END), 0) AS yesterday,",
            "  COALESCE(SUM(CASE WHEN record_time >= DATE_FORMAT(CURDATE(), '%Y-%m-01') THEN consumption ELSE 0 END), 0) AS month_total,",
            "  COALESCE(SUM(consumption), 0) AS grand_total",
            "FROM energy_record",
            "WHERE deleted = 0"
    })
    Map<String, Object> sumStatistics();

    /** 今日总用电量 SUM */
    @Select("SELECT COALESCE(SUM(consumption), 0) FROM energy_record WHERE record_time >= #{since} AND deleted = 0")
    java.math.BigDecimal sumConsumptionSince(@Param("since") java.time.LocalDateTime since);

    /** 近 N 天按日期分组能耗趋势 */
    @Select("SELECT DATE(record_time) AS date, COALESCE(SUM(consumption), 0) AS totalEnergy FROM energy_record WHERE record_time >= #{since} AND deleted = 0 GROUP BY DATE(record_time) ORDER BY date ASC")
    List<Map<String, Object>> energyTrendByDay(@Param("since") java.time.LocalDateTime since);

    /** 能耗排名：按设备分组 SUM 取 TOP N */
    @Select("SELECT device_id, COALESCE(SUM(consumption), 0) AS totalEnergy FROM energy_record WHERE record_time >= #{since} AND deleted = 0 GROUP BY device_id ORDER BY totalEnergy DESC LIMIT #{limit}")
    List<Map<String, Object>> topEnergyDevice(@Param("since") java.time.LocalDateTime since, @Param("limit") int limit);
}
