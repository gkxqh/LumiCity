package com.ccb.lighting.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.alarm.dto.AlarmQueryDTO;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.alarm.mapper.AlarmRecordMapper;
import com.ccb.lighting.module.alarm.service.AlarmRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 告警记录 Service 实现类
 *
 * <p>关键实现：
 * - 分页：按告警时间倒序，最新告警排前面，便于运维第一时间看到
 * - 处理：更新 status + handleUser + handleTime 三字段，形成闭环
 * - 统计：分别 selectCount 不同状态的告警数</p>
 */
@Service
@RequiredArgsConstructor
public class AlarmRecordServiceImpl implements AlarmRecordService {

    /** 告警记录 Mapper，构造器注入 */
    private final AlarmRecordMapper alarmRecordMapper;

    /**
     * 分页查询告警记录
     * 按告警时间倒序，最新告警优先展示
     */
    @Override
    public IPage<AlarmRecord> pageList(PageQuery query) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AlarmRecord::getAlarmTime);
        return alarmRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询告警详情
     */
    @Override
    public AlarmRecord getById(Long id) {
        AlarmRecord record = alarmRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return record;
    }

    @Override
    public IPage<AlarmRecord> pageListByQuery(AlarmQueryDTO query) {
        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<>();
        if(query.getAlarmType()!=null && !query.getAlarmType().isEmpty()){
            wrapper.eq(AlarmRecord::getAlarmType,query.getAlarmType());
        }
        if(query.getAlarmLevel()!=null){
            wrapper.eq(AlarmRecord::getAlarmLevel,query.getAlarmLevel());
        }
        if(query.getStatus()!=null){
            wrapper.eq(AlarmRecord::getStatus,query.getStatus());
        }

        wrapper.orderByDesc(AlarmRecord::getAlarmTime);
        return alarmRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );

    }

    /**
     * 处理告警
     * 三步：校验告警存在 → 更新状态/处理人/处理时间 → 入库
     */
    @Override
    public void handle(Long id, Integer status, String handleUser) {
        // 1. 校验告警是否存在
        AlarmRecord record = alarmRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 2. 状态校验：仅允许 1 处理中 / 2 已闭环
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("status 仅支持 1 处理中 或 2 已闭环");
        }
        // 3. 更新字段：状态、处理人、处理时间
        record.setStatus(status);
        record.setHandleUser(handleUser);
        record.setHandleTime(LocalDateTime.now());
        alarmRecordMapper.updateById(record);
    }

    /**
     * 告警统计汇总
     * 分别查询"未处理 / 处理中 / 已闭环"三类告警的数量
     */
    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        // 未处理告警数
        Long pending = alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getStatus, 0)
        );
        // 处理中告警数
        Long processing = alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getStatus, 1)
        );
        // 已闭环告警数
        Long closed = alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getStatus, 2)
        );
        // 总告警数
        Long total = alarmRecordMapper.selectCount(null);

        result.put("pending", pending);
        result.put("processing", processing);
        result.put("closed", closed);
        result.put("total", total);
        return result;
    }
}
