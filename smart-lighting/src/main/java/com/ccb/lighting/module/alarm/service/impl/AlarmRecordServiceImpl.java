package com.ccb.lighting.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.handler.AlarmWebSocketHandler;
import com.ccb.lighting.module.alarm.dto.AlarmQueryDTO;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.alarm.mapper.AlarmRecordMapper;
import com.ccb.lighting.module.alarm.service.AlarmRecordService;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.service.SysUserService;
import com.ccb.lighting.module.system.service.impl.SysUserServiceImpl;
import com.ccb.lighting.module.workorder.entity.WorkOrder;
import com.ccb.lighting.module.workorder.service.WorkOrderService;
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

    /** 告警 WebSocket 处理器，构造器注入，用于实时推送 */
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final SysUserService sysUserService;

    /** 工单服务构造器注入
     *
     */
    private final WorkOrderService workOrderService;


    /**
     * 新增告警记录
     * 入库后通过 WebSocket 广播 "alarm_new" 事件给所有在线客户端。
     */
    @Override
    public void add(AlarmRecord record) {
        // 默认值填充：告警时间缺省取当前；状态缺省为 0 未处理
        if (record.getAlarmTime() == null) {
            record.setAlarmTime(LocalDateTime.now());
        }
        if (record.getStatus() == null) {
            record.setStatus(0);
        }
        alarmRecordMapper.insert(record);
        // 实时推送（时间字段转字符串，避免 LocalDateTime 序列化差异）
        alarmWebSocketHandler.broadcast(buildMessage("alarm_new", record));
    }

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

        return alarmRecordMapper.selectAlarmPage(
                new Page<>(query.getCurrent(), query.getSize()),
                query);

    }


     /**
     * 处理告警（分配处理人）
     * 三步：校验告警存在 → 更新状态/处理人/处理时间 → 入库
     */
    @Override
    public void handle(Long id, Integer status, String handleUser, String handleResult) {
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
        if(status == 1){
            //不允许处理人为空
            if(handleUser == null || handleUser.isBlank()){
                throw new BusinessException("处理人不能为空！");
            }
            record.setHandleUser(handleUser);
            record.setStatus(1);
            //新增功能：自动创建工单
            workOrderService.createFromAlarm(record, handleUser);
        } else {
            //不允许处理结果为空
            if(handleResult == null || handleResult.isBlank()){
                throw new BusinessException("处理结果不能为空！");
            }
            //校验关联工单是否已经完成
            WorkOrder related = workOrderService.getByAlarmId(record.getId());
            if(related != null && related.getStatus() != 2){
                throw new BusinessException("关联工单未完成，无法处理告警！");
            }
            record.setHandleResult(handleResult);
            record.setHandleTime(LocalDateTime.now());
            record.setStatus(2);
        }
        alarmRecordMapper.updateById(record);
        // 实时推送状态变更
        alarmWebSocketHandler.broadcast(buildMessage("alarm_handled", record));
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

    /**
     * 构造 WebSocket 推送消息体
     * 结构：{ event: "alarm_new"/"alarm_handled", data: { id, deviceId, alarmType, ... }, time }
     * 时间字段统一转字符串，避免 LocalDateTime 在不同 Jackson 配置下序列化不一致。
     */
    private Map<String, Object> buildMessage(String event, AlarmRecord record) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", record.getId());
        data.put("deviceId", record.getDeviceId());
        data.put("poleId", record.getPoleId());
        data.put("alarmType", record.getAlarmType());
        data.put("alarmLevel", record.getAlarmLevel());
        data.put("alarmContent", record.getAlarmContent());
        data.put("alarmTime", record.getAlarmTime() != null ? record.getAlarmTime().toString() : null);
        data.put("status", record.getStatus());
        data.put("handleUser", record.getHandleUser());
        Map<String, Object> message = new HashMap<>();
        message.put("event", event);
        message.put("data", data);
        message.put("time", LocalDateTime.now().toString());
        return message;
    }
}
