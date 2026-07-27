package com.ccb.lighting.module.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.module.publish.dto.LedProgramQueryDTO;
import com.ccb.lighting.module.publish.entity.LedProgram;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * LED 节目 Mapper 接口
 *
 * <p>继承 BaseMapper<LedProgram> 自动拥有单表 CRUD 方法。</p>
 */
@Mapper
public interface LedProgramMapper extends BaseMapper<LedProgram> {

    /**
     * 分页查询节目列表（带屏幕设备名称），left join dev_device
     */
    IPage<LedProgram> pageListByQuery(Page<LedProgram> page, @Param("query") LedProgramQueryDTO query);
}
