package com.ccb.lighting.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.alarm.dto.AlarmQueryDTO;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 告警记录 Mapper 接口
 *
 * <p>继承 BaseMapper<AlarmRecord> 自动拥有单表 CRUD。
 * 统计接口可用 selectCount + 条件构造器实现。</p>
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {

    @Select({
            "<script>",
            "SELECT ar.*, d.device_name AS deviceName",
            "FROM alarm_record ar",
            "LEFT JOIN dev_device d ON ar.device_id = d.device_code",
            "WHERE ar.deleted = 0",
            "<if test='query.alarmType != null and query.alarmType != \"\"'> AND ar.alarm_type = #{query.alarmType} </if>",
            "<if test='query.alarmLevel != null'> AND ar.alarm_level = #{query.alarmLevel} </if>",
            "<if test='query.status != null'> AND ar.status = #{query.status} </if>",
            "ORDER BY ar.alarm_time DESC",
            "</script>"
    })
    IPage<AlarmRecord> selectAlarmPage(IPage<AlarmRecord> page,
                                       @Param("query") AlarmQueryDTO query);
}
