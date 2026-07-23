package com.ccb.lighting.module.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.publish.entity.LedProgram;
import org.apache.ibatis.annotations.Mapper;

/**
 * LED 节目 Mapper 接口
 *
 * <p>继承 BaseMapper<LedProgram> 自动拥有单表 CRUD 方法。</p>
 */
@Mapper
public interface LedProgramMapper extends BaseMapper<LedProgram> {
}
