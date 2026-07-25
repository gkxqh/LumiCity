package com.ccb.lighting.module.energy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}
