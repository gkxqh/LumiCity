package com.ccb.lighting.module.alarm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.alarm.dto.AlarmQueryDTO;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;

import java.util.Map;

/**
 * 告警记录 Service 接口
 *
 * <p>方法清单：
 * - pageList：分页查询告警记录（支持按类型、级别、状态筛选）
 * - getById：根据 id 查告警详情
 * - handle：处理告警（更新状态、记录处理人、处理时间）
 * - statistics：告警统计（各状态数量、各类型数量等）</p>
 */
public interface AlarmRecordService {

    /**
     * 新增告警记录
     * 设备发生异常时由系统调用（或手动触发模拟），入库后通过 WebSocket 广播给所有在线客户端。
     *
     * @param record 告警记录（alarmTime/status 未传时自动填充）
     */
    void add(AlarmRecord record);

    /**
     * 分页查询告警记录
     *
     * @param query 分页参数
     * @return 分页对象
     */
    IPage<AlarmRecord> pageList(PageQuery query);

    /**
     * 根据 id 查询告警详情
     *
     * @param id 告警 ID
     * @return 告警记录
     */
    AlarmRecord getById(Long id);

    IPage<AlarmRecord> pageListByQuery(AlarmQueryDTO query);

     /**
     * 处理告警(分配处理人)
     * 运维人员介入后调用：更新状态为处理中，记录处理人与处理时间
     *
     * @param id          告警 ID
     * @param status      目标状态（1处理中 / 2已闭环）
     * @param handleUser  处理人
     */
    void handle(Long id, Integer status, String handleUser, String handleResult);

    /**
     * 告警统计汇总
     * 返回各状态、各类型、各级别的告警数量，用于数据大盘
     *
     * @return 统计结果 Map
     */
    Map<String, Object> statistics();
}
