package com.ccb.lighting.module.lighting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.lighting.entity.LightCommandLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 照明控制指令日志 Mapper
 */
@Mapper
public interface LightCommandLogMapper extends BaseMapper<LightCommandLog> {
}
