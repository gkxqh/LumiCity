package com.ccb.lighting.module.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper 接口
 *
 * <p>继承 BaseMapper<WorkOrder> 自动拥有单表 CRUD 方法。</p>
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}
