package com.ccb.lighting.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.alarm.dto.AlarmQueryDTO;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

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

    /** 近 N 天按日期分组告警趋势 */
    @Select("SELECT DATE(alarm_time) AS date, COUNT(*) AS count FROM alarm_record WHERE alarm_time >= #{since} AND deleted = 0 GROUP BY DATE(alarm_time) ORDER BY date ASC")
    List<Map<String, Object>> countByDaySince(@Param("since") java.time.LocalDateTime since);

    /** 按告警类型分组统计 */
    @Select("SELECT alarm_type AS typeKey, COUNT(*) AS count FROM alarm_record WHERE deleted = 0 GROUP BY alarm_type ORDER BY count DESC")
    List<Map<String, Object>> countByType();

    /** 取最新 N 条告警（带设备名） */
    @Select("SELECT ar.*, d.device_name AS deviceName FROM alarm_record ar LEFT JOIN dev_device d ON ar.device_id = d.device_code WHERE ar.deleted = 0 ORDER BY ar.alarm_time DESC LIMIT #{limit}")
    List<AlarmRecord> latestAlarmList(@Param("limit") int limit);
}
