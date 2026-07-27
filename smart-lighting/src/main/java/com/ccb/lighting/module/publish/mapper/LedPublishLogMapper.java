package com.ccb.lighting.module.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.publish.entity.LedPublishLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * LED 节目发布记录 Mapper 接口
 *
 * <p>继承 BaseMapper 自动拥有单表 CRUD。
 * 分页查询发布历史由 Service 层配合 PageQuery 实现。</p>
 */
@Mapper
public interface LedPublishLogMapper extends BaseMapper<LedPublishLog> {
}
