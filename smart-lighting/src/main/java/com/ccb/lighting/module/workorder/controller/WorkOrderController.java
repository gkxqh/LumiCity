package com.ccb.lighting.module.workorder.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.workorder.dto.WorkOrderQueryDTO;
import com.ccb.lighting.module.workorder.entity.WorkOrder;
import com.ccb.lighting.module.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单 Controller
 *
 * <p>路径前缀 /workorder，提供工单的创建、查询、派单、处理、完成接口。
 * 工单是运维流程的核心载体，串联设备告警 → 工单创建 → 派单 → 处理 → 验收的完整闭环。</p>
 *
 * <p>接口列表：
 * - GET  /workorder/page         分页查询工单
 * - GET  /workorder/{id}          查询工单详情
 * - POST /workorder                新增工单
 * - PUT  /workorder/assign/{id}   派单（指定处理人）
 * - PUT  /workorder/handle/{id}   开始处理
 * - PUT  /workorder/finish/{id}   完成工单</p>
 */
@RestController
@RequestMapping("/workorder")
@RequiredArgsConstructor
public class WorkOrderController {

    /** 工单 Service，构造器注入 */
    private final WorkOrderService workOrderService;

    /**
     * 分页查询工单列表
     *
     * @param query 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<WorkOrder>> page(WorkOrderQueryDTO query) {
        return Result.success(workOrderService.pageListByQuery(query));
    }

    /**
     * 根据 id 查询工单详情
     *
     * @param id 工单 ID
     * @return 工单信息
     */
    @GetMapping("/{id}")
    public Result<WorkOrder> getById(@PathVariable Long id) {
        return Result.success(workOrderService.getById(id));
    }

    /**
     * 新增工单
     * 系统自动生成工单编号，状态默认 0 待处理
     *
     * @param order 工单信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody WorkOrder order) {
        workOrderService.add(order);
        return Result.success();
    }

    /**
     * 派单
     * 将工单指派给某运维人员，状态从 0 待处理 → 1 处理中
     *
     * <p>请求示例：PUT /workorder/assign/100?assigneeId=1001</p>
     *
     * @param id          工单 ID
     * @param assigneeId  指派人 ID
     * @return 操作结果
     */
    @PutMapping("/assign/{id}")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        workOrderService.assign(id, assigneeId);
        return Result.success();
    }

    /**
     * 处理工单（处理即完成）
     * 填写处理备注后，状态从 1 处理中 → 2 已完成，记录完成时间；
     * 告警关联工单同时推送 workorder_finished 事件
     *
     * @param id            工单 ID
     * @param handleRemark  处理备注
     * @return 操作结果
     */
    @PutMapping("/handle/{id}")
    public Result<Void> handle(@PathVariable Long id, @RequestParam(required = false) String handleRemark) {
        workOrderService.handle(id, handleRemark);
        return Result.success();
    }

    /**
     * 完成工单
     * 处理完毕，状态从 1 处理中 → 2 已完成，记录完成时间
     *
     * @param id 工单 ID
     * @return 操作结果
     */
    @PutMapping("/finish/{id}")
    public Result<Void> finish(@PathVariable Long id) {
        workOrderService.finish(id);
        return Result.success();
    }

    /**
     * 根据告警 ID 查询关联工单
     * 告警页面在允许编写处理意见之前，先查询关联工单是否已完成
     *
     * @param alarmId 告警记录 ID
     * @return 工单信息（可能为 null）
     */
    @GetMapping("/by-alarm/{alarmId}")
    public Result<WorkOrder> getByAlarmId(@PathVariable Long alarmId) {
        return Result.success(workOrderService.getByAlarmId(alarmId));
    }

}
