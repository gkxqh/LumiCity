package com.ccb.lighting.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警记录 Mapper 接口
 *
 * <p>继承 BaseMapper<AlarmRecord> 自动拥有单表 CRUD。
 * 统计接口可用 selectCount + 条件构造器实现。</p>
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {
}
