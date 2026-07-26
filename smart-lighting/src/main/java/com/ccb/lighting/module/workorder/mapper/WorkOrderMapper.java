package com.ccb.lighting.module.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.workorder.dto.WorkOrderQueryDTO;
import com.ccb.lighting.module.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工单 Mapper 接口
 *
 * <p>继承 BaseMapper<WorkOrder> 自动拥有单表 CRUD 方法。</p>
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /** 今日新增工单数 */
    @Select("SELECT COUNT(*) FROM work_order WHERE DATE(create_time) = CURDATE() AND deleted = 0")
    long countToday();

    /** 按状态分组统计 */
    @Select("SELECT status, COUNT(*) AS count FROM work_order WHERE deleted = 0 GROUP BY status ORDER BY status ASC")
    List<Map<String, Object>> countByStatus();

    @Select({
            "<script>",
            "SELECT * FROM work_order",
            "WHERE deleted = 0",
            "<if test='query.orderType != null and query.orderType != \"\"'> AND order_type = #{query.orderType} </if>",
            "<if test='query.status != null'> AND status = #{query.status} </if>",
            "<if test='query.deviceId != null and query.deviceId != \"\"'> AND device_id LIKE CONCAT('%', #{query.deviceId}, '%') </if>",
            "ORDER BY create_time DESC",
            "</script>"
    })
    IPage<WorkOrder> selectWorkOrderPage(IPage<WorkOrder> page, @Param("query") WorkOrderQueryDTO query);
}
