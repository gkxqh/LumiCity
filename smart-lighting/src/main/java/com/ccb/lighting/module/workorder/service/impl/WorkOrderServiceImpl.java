package com.ccb.lighting.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.workorder.entity.WorkOrder;
import com.ccb.lighting.module.workorder.mapper.WorkOrderMapper;
import com.ccb.lighting.module.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * 处理工单
     * 状态推进到 1 处理中（兼容从待处理状态直接进入处理中）
     */
    @Override
    public void handle(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 已完成或已验收不可再处理
        if (order.getStatus() != null && order.getStatus() >= 2) {
            throw new BusinessException("工单已完成，不可再处理");
        }
        order.setStatus(1);
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
        order.setStatus(2);
        order.setFinishTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
    }
}
