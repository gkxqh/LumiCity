package com.ccb.lighting.module.energy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 能耗记录 Mapper 接口
 *
 * <p>继承 BaseMapper<EnergyRecord> 自动拥有单表 CRUD。
 * 趋势查询、统计汇总均基于 BaseMapper 的 selectList + 条件构造器实现。</p>
 */
@Mapper
public interface EnergyRecordMapper extends BaseMapper<EnergyRecord> {
}
