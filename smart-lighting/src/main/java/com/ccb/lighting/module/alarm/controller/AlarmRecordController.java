package com.ccb.lighting.module.alarm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.alarm.dto.AlarmQueryDTO;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.alarm.service.AlarmRecordService;
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

import java.util.Map;

/**
 * 告警记录 Controller
 *
 * <p>路径前缀 /alarm，提供告警查询、处理、统计接口。
 * 设备发生异常时由系统自动生成告警记录，运维人员通过本模块处理闭环。</p>
 *
 * <p>接口列表：
 * - GET /alarm/page        分页查询告警列表
 * - GET /alarm/{id}        查询告警详情
 * - PUT /alarm/handle      处理告警（更新状态、处理人）
 * - GET /alarm/statistics  告警统计汇总（数据大盘用）</p>
 */
@RestController
@RequestMapping("/alarm")
@RequiredArgsConstructor
public class AlarmRecordController {

    /** 告警记录 Service，构造器注入 */
    private final AlarmRecordService alarmRecordService;

    /**
     * 新增告警记录
     * 设备发生异常时由系统调用；也可用于前端"模拟告警"按钮触发，便于演示 WebSocket 推送。
     * 入库后 Service 会通过 WebSocket 广播 alarm_new 事件给所有在线客户端。
     *
     * @param record 告警信息（@Valid 校验 deviceId/alarmType/alarmLevel 必填）
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AlarmRecord record) {
        alarmRecordService.add(record);
        return Result.success();
    }


    /**
     * 根据 id 查询告警详情
     *
     * @param id 告警 ID
     * @return 告警记录
     */
    @GetMapping("/{id}")
    public Result<AlarmRecord> getById(@PathVariable Long id) {
        return Result.success(alarmRecordService.getById(id));
    }

    /**
     * 页面查询告警
     * 返回值：告警记录
     */
    @GetMapping("/page")
    public Result<IPage<AlarmRecord>>page(AlarmQueryDTO query){
        return Result.success(alarmRecordService.pageListByQuery(query));
    }

    /**
     * 处理告警（分配处理人）
     *
     * <p>请求示例：PUT /alarm/handle?id=100&status=2&handleUser=zhangsan
     * status=1 表示运维已接手处理中，status=2 表示已闭环。</p>
     *
     * @param id          告警 ID
     * @param status      目标状态（1处理中 / 2已闭环）
     * @param handleUser  处理人用户名
     * @return 操作结果
     */
    @PutMapping("/handle")
    public Result<Void> handle(
            @RequestParam Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String handleUser,
            @RequestParam(required = false) String handleResult) {
        alarmRecordService.handle(id, status, handleUser, handleResult);
        return Result.success();
    }

    /**
     * 告警统计汇总
     * 返回各状态告警数量，用于数据大盘展示
     *
     * @return 统计结果 Map
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(alarmRecordService.statistics());
    }
}
