package com.ccb.lighting.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.handler.AlarmWebSocketHandler;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.service.SysUserService;
import com.ccb.lighting.module.workorder.dto.WorkOrderQueryDTO;
import com.ccb.lighting.module.workorder.entity.WorkOrder;
import com.ccb.lighting.module.workorder.mapper.WorkOrderMapper;
import com.ccb.lighting.module.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 工单 Service 实现类
 *
 * <p>关键实现：
 * - add：自动生成工单编号（WO-yyyyMMdd-随机数），状态默认 0 待处理
 * - assign：派单，状态 0 → 1
 * - handle：开始处理，状态推进到 1（兼容从待处理直接进入处理中）
 * - finish：完成，状态 1 → 2，记录 finishTime</p>
 */
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    /** 工单 Mapper，构造器注入 */
    private final WorkOrderMapper workOrderMapper;
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final SysUserService sysUserService;

    /**
     * 分页查询工单列表
     * 按创建时间倒序，最新工单排前面
     */
    @Override
    public IPage<WorkOrder> pageList(PageQuery query) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        return workOrderMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询工单
     */
    @Override
    public WorkOrder getById(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return order;
    }

    /**
     * 新增工单
     * 自动生成工单编号 WO-yyyyMMdd-4位随机数
     * 默认状态 0 待处理，优先级默认 2 中
     */
    @Override
    public void add(WorkOrder order) {
        // 自动生成工单编号：WO-20260722-1234
        String orderNo = "WO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        order.setOrderNo(orderNo);
        // 默认状态 0 待处理
        if (order.getStatus() == null) {
            order.setStatus(0);
        }
        // 默认优先级 2 中
        if (order.getPriority() == null) {
            order.setPriority(2);
        }
        workOrderMapper.insert(order);
    }

    /**
     * 派单
     * 状态从 0 待处理 推进到 1 处理中，并记录指派人
     */
    @Override
    public void assign(Long id, Long assigneeId) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 状态校验：仅待处理工单可派单
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BusinessException("仅待处理状态工单可派单");
        }
        order.setAssigneeId(assigneeId);
        order.setStatus(1);
        workOrderMapper.updateById(order);
    }

    /**
     * 处理工单（处理即完成）
     * 填写处理备注后，状态从 1 处理中 直接推进到 2 已完成，
     * 记录处理备注和完成时间；
     * 若为告警关联工单，广播 workorder_finished 给运维人员
     */
    @Override
    public void handle(Long id, String handleRemark) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 仅处理中状态可处理完成
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("仅处理中状态工单可处理");
        }
        // 保存处理备注
        order.setHandleRemark(handleRemark);
        order.setStatus(2);
        order.setFinishTime(LocalDateTime.now());
        // 如果是告警关联的工单，推送完成通知给运维人员
        if (order.getAlarmId() != null) {
            Map<String, Object> wsMsg = new HashMap<>();
            Map<String, Object> wsData = new HashMap<>();
            wsData.put("id", order.getId());
            wsData.put("orderNo", order.getOrderNo());
            wsData.put("title", order.getTitle());
            wsData.put("alarmId", order.getAlarmId());
            wsData.put("handleRemark", handleRemark);
            wsMsg.put("event", "workorder_finished");
            wsMsg.put("data", wsData);
            wsMsg.put("time", LocalDateTime.now().toString());
            // 推送给所有在线用户（运维人员会收到）
            alarmWebSocketHandler.broadcast(wsMsg);
        }
        workOrderMapper.updateById(order);
    }

    /**
     * 完成工单
     * 状态从 1 处理中 推进到 2 已完成，记录完成时间
     */
    @Override
    public void finish(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 状态校验：仅处理中状态可完成
        if (order.getStatus() == null || order.getStatus() != 1) {
            throw new BusinessException("仅处理中状态工单可完成");
        }
        // 如果是告警关联的工单，推送完成通知给运维人员
        if (order.getAlarmId() != null) {
            Map<String, Object> wsMsg = new HashMap<>();
            Map<String, Object> wsData = new HashMap<>();
            wsData.put("id", order.getId());
            wsData.put("orderNo", order.getOrderNo());
            wsData.put("title", order.getTitle());
            wsData.put("alarmId", order.getAlarmId());
            wsMsg.put("event", "workorder_finished");
            wsMsg.put("data", wsData);
            wsMsg.put("time", LocalDateTime.now().toString());
            // 推送给所有在线用户（运维人员会收到）
            alarmWebSocketHandler.broadcast(wsMsg);
        }
        order.setStatus(2);
        order.setFinishTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
    }

    @Override
    public void createFromAlarm(AlarmRecord alarmRecord, String handleUser) {
        // 1. 组装工单
        WorkOrder order = new WorkOrder();
        order.setOrderType("REPAIR");

        String typeText = switch (alarmRecord.getAlarmType()) {
            case "OFFLINE" -> "离线告警";
            case "OVERVOLTAGE" -> "过压告警";
            case "OVERCURRENT" -> "过流告警";
            case "ABNORMAL" -> "其他异常";
            default -> "无法识别的告警，请联系管理员！";
        };
        String deviceName = alarmRecord.getDeviceName();
        order.setTitle("维修工单：" + (deviceName != null ? deviceName : alarmRecord.getDeviceId()) + " - " + typeText);
        order.setDescription(alarmRecord.getAlarmContent());
        order.setDeviceId(alarmRecord.getDeviceId());
        order.setPoleId(alarmRecord.getPoleId());
        order.setAlarmId(alarmRecord.getId());
        order.setPriority(alarmRecord.getAlarmLevel());
        order.setStatus(1);

        // 2. 设置指派人
        SysUser assignee = sysUserService.findByUsername(handleUser);
        if (assignee != null) {
            order.setAssigneeId(assignee.getId());
        }

        // 3. 入库
        this.add(order);

        // 4. WebSocket 定向推送给处理人
        Map<String, Object> wsMsg = new HashMap<>();
        Map<String, Object> wsData = new HashMap<>();
        wsData.put("id", order.getId());
        wsData.put("orderNo", order.getOrderNo());
        wsData.put("title", order.getTitle());
        wsData.put("alarmId", alarmRecord.getId());
        wsMsg.put("event", "workorder_new");
        wsMsg.put("data", wsData);
        wsMsg.put("time", LocalDateTime.now().toString());
        alarmWebSocketHandler.sendToUser(handleUser, wsMsg);
    }

    @Override
    public WorkOrder getByAlarmId(Long alarmId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getAlarmId, alarmId);
        return workOrderMapper.selectOne(wrapper);
    }

    @Override
    public IPage<WorkOrder> pageListByQuery(WorkOrderQueryDTO query) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        if(query.getOrderType()!=null && !query.getOrderType().isEmpty()){
            wrapper.eq(WorkOrder::getOrderType,query.getOrderType());
        }
        if(query.getStatus() != null){
            wrapper.eq(WorkOrder::getStatus,query.getStatus());
        }
        if(query.getDeviceId()!=null && !query.getDeviceId().isEmpty()){
            wrapper.eq(WorkOrder::getDeviceId,query.getDeviceId());
        }

        wrapper.orderByDesc(WorkOrder::getCreateTime);

        return workOrderMapper.selectWorkOrderPage(new Page<>(query.getCurrent(), query.getSize()), query);

    }
}
