package com.ccb.lighting.module.lighting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.lighting.entity.LightStrategy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照明策略 Mapper 接口
 *
 * <p>继承 BaseMapper<LightStrategy> 自动拥有单表 CRUD 方法。
 * 本表为单表操作，无需手写 SQL。</p>
 *
 * <p>@Mapper 让 Spring 生成代理实现类，在 Service 里直接注入使用。</p>
 */
@Mapper
public interface LightStrategyMapper extends BaseMapper<LightStrategy> {
}
