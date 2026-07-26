package com.ccb.lighting.module.workorder.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.workorder.dto.WorkOrderQueryDTO;
import com.ccb.lighting.module.workorder.entity.WorkOrder;

import java.util.List;

/**
 * 工单 Service 接口
 *
 * <p>方法清单：
 * - pageList：分页查询工单列表
 * - getById：根据 id 查工单详情
 * - add：新增工单（自动生成工单编号）
 * - assign：派单（指定处理人，状态 0→1）
 * - handle：处理（运维人员开始处理，状态推进到 1）
 * - finish：完成（处理完毕，状态 1→2，记录完成时间）</p>
 */
public interface WorkOrderService {

    /**
     * 分页查询工单列表
     *
     * @param query 分页参数
     * @return 分页对象
     */
    IPage<WorkOrder> pageList(PageQuery query);

    /**
     * 根据 id 查询工单详情
     *
     * @param id 工单 ID
     * @return 工单实体
     */
    WorkOrder getById(Long id);

    /**
     * 新增工单
     * 系统自动生成工单编号，状态默认 0 待处理
     *
     * @param order 工单信息
     */
    void add(WorkOrder order);

    /**
     * 派单
     * 指定处理人，状态从 0 待处理 推进到 1 处理中
     *
     * @param id          工单 ID
     * @param assigneeId  指派人 ID
     */
    void assign(Long id, Long assigneeId);

    /**
     * 处理工单
     * 填写处理备注后，状态从 1 处理中 推进到 2 已完成，记录完成时间；
     * 告警关联工单同时广播 WebSocket workorder_finished 事件
     *
     * @param id            工单 ID
     * @param handleRemark  处理备注（处理过程/结果描述）
     */
    void handle(Long id, String handleRemark);

    /**
     * 完成工单
     * 处理完毕，状态从 1 处理中 推进到 2 已完成，记录完成时间
     *
     * @param id 工单 ID
     */
    void finish(Long id);

    /**
     * 根据告警记录自动创建维修工单
     * 包含：工单组装、入库、WebSocket 定向推送给处理人
     *
     * @param alarmRecord     告警记录（从中提取设备、内容等信息）
     * @param handleUser 指定的处理人用户名
     *
     */
    void createFromAlarm(AlarmRecord alarmRecord,String handleUser);

    /**
     * 根据告警ID查询关联工单
     * 告警页面写处理意见时，需检验工单是否已完成
     */
    WorkOrder getByAlarmId(Long alarmId);

    IPage<WorkOrder> pageListByQuery(WorkOrderQueryDTO query);
}
