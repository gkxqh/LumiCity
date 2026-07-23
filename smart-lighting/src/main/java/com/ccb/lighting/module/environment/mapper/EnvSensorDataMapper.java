package com.ccb.lighting.module.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.environment.entity.EnvSensorData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 环境传感器数据 Mapper 接口
 *
 * <p>继承 BaseMapper<EnvSensorData> 自动拥有单表 CRUD。</p>
 */
@Mapper
public interface EnvSensorDataMapper extends BaseMapper<EnvSensorData> {
}
