package com.ccb.lighting.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.alarm.mapper.AlarmRecordMapper;
import com.ccb.lighting.module.device.entity.DevDevice;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.mapper.DevDeviceMapper;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import com.ccb.lighting.module.energy.mapper.EnergyRecordMapper;
import com.ccb.lighting.module.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据大盘 Controller
 *
 * <p>路径前缀 /dashboard，提供首页数据大盘的聚合查询接口。
 * 大盘不建表，而是聚合多个模块的数据：设备、告警、能耗、工单、环境，做综合统计展示。</p>
 *
 * <p>接口列表：
 * - GET /dashboard/overview              核心指标汇总
 * - GET /dashboard/alarm/trend           近 N 天告警趋势
 * - GET /dashboard/energy/trend          近 N 天能耗趋势
 * - GET /dashboard/device/type-dist      设备类型分布
 * - GET /dashboard/alarm/category        告警分类统计
 * - GET /dashboard/latest-alarm          最新告警列表
 * - GET /dashboard/workorder/stats       工单快照
 * - GET /dashboard/latest-env            最新环境数据</p>
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AlarmRecordMapper alarmRecordMapper;
    private final DevDeviceMapper devDeviceMapper;
    private final DevPoleMapper devPoleMapper;
    private final EnergyRecordMapper energyRecordMapper;
    private final WorkOrderMapper workOrderMapper;

    private static final Map<String, String> DEVICE_TYPE_NAMES = Map.of(
            "LIGHT", "照明灯",
            "CAMERA", "摄像头",
            "SENSOR", "传感器",
            "LED_SCREEN", "LED屏",
            "BROADCAST", "广播"
    );

    private static final Map<String, String> ALARM_TYPE_NAMES = Map.of(
            "OFFLINE", "离线告警",
            "OVERVOLTAGE", "过压告警",
            "OVERCURRENT", "过流告警",
            "ABNORMAL", "其他异常"
    );

    private static final Map<Integer, String> ALARM_LEVEL_NAMES = Map.of(
            1, "严重",
            2, "重要",
            3, "一般"
    );

    /**
     * 核心指标汇总
     *
     * <p>返回首页大盘展示的核心数据：
     * - deviceTotal：设备总数
     * - deviceOnline：在线设备数
     * - deviceFault：故障设备数
     * - alarmPending：未处理告警数
     * - todayEnergy：今日总用电量 (kWh)
     * - onlineRate：在线率
     * - poleTotal：灯杆总数
     * - workOrderToday：今日新增工单数</p>
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> result = new HashMap<>();

        // 1. 设备总数
        Long deviceTotal = devDeviceMapper.selectCount(null);

        // 2. 在线设备数：status=1
        Long deviceOnline = devDeviceMapper.selectCount(
                new LambdaQueryWrapper<DevDevice>().eq(DevDevice::getStatus, 1)
        );

        // 3. 故障设备数：status=2
        Long deviceFault = devDeviceMapper.selectCount(
                new LambdaQueryWrapper<DevDevice>().eq(DevDevice::getStatus, 2)
        );

        // 4. 未处理告警数：status=0
        Long alarmPending = alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getStatus, 0)
        );

        // 5. 今日总用电量：SUM(consumption) 真实聚合
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        BigDecimal todayEnergy = energyRecordMapper.sumConsumptionSince(todayStart);

        // 6. 灯杆总数
        Long poleTotal = devPoleMapper.selectCount(null);

        // 7. 今日新增工单数
        long workOrderToday = workOrderMapper.countToday();

        result.put("deviceTotal", deviceTotal);
        result.put("deviceOnline", deviceOnline);
        result.put("deviceFault", deviceFault);
        result.put("alarmPending", alarmPending);
        result.put("todayEnergy", todayEnergy);
        result.put("poleTotal", poleTotal);
        result.put("workOrderToday", workOrderToday);
        // 在线率
        if (deviceTotal != null && deviceTotal > 0) {
            result.put("onlineRate", String.format("%.2f", deviceOnline * 100.0 / deviceTotal) + "%");
        } else {
            result.put("onlineRate", "0%");
        }
        return Result.success(result);
    }

    /**
     * 近 N 天告警趋势（默认 7 天）
     *
     * <p>按日期 GROUP BY 真实聚合，返回 [{ date, count }]</p>
     */
    @GetMapping("/alarm/trend")
    public Result<List<Map<String, Object>>> alarmTrend(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Map<String, Object>> dbResult = alarmRecordMapper.countByDaySince(since);

        // 补全天数（数据库无记录的天 = 0）
        Map<String, Object> lookup = new HashMap<>();
        for (Map<String, Object> row : dbResult) {
            lookup.put(row.get("date").toString(), row.get("count"));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> day = new HashMap<>();
            day.put("date", date);
            day.put("count", lookup.getOrDefault(date, 0L));
            result.add(day);
        }
        return Result.success(result);
    }

    /**
     * 近 N 天能耗趋势（默认 7 天）
     *
     * <p>按日期 SUM(consumption) 聚合，返回 [{ date, totalEnergy }]</p>
     */
    @GetMapping("/energy/trend")
    public Result<List<Map<String, Object>>> energyTrend(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDate.now().minusDays(days - 1).atStartOfDay();
        List<Map<String, Object>> dbResult = energyRecordMapper.energyTrendByDay(since);

        // 补全天数
        Map<String, Object> lookup = new HashMap<>();
        for (Map<String, Object> row : dbResult) {
            lookup.put(row.get("date").toString(), row.get("totalEnergy"));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> day = new HashMap<>();
            day.put("date", date);
            day.put("totalEnergy", lookup.getOrDefault(date, BigDecimal.ZERO));
            result.add(day);
        }
        return Result.success(result);
    }

    /**
     * 设备类型分布
     *
     * <p>按 device_type 分组统计，返回 [{ typeKey, typeName, count }]</p>
     */
    @GetMapping("/device/type-dist")
    public Result<List<Map<String, Object>>> deviceTypeDistribution() {
        List<Map<String, Object>> dbResult = devDeviceMapper.countByType();
        for (Map<String, Object> row : dbResult) {
            String key = (String) row.get("typeKey");
            row.put("typeName", DEVICE_TYPE_NAMES.getOrDefault(key, key));
        }
        return Result.success(dbResult);
    }

    /**
     * 告警分类统计
     *
     * <p>按 alarm_type 分组统计，返回 [{ typeKey, typeName, count }]</p>
     */
    @GetMapping("/alarm/category")
    public Result<List<Map<String, Object>>> alarmCategory() {
        List<Map<String, Object>> dbResult = alarmRecordMapper.countByType();
        for (Map<String, Object> row : dbResult) {
            String key = (String) row.get("typeKey");
            row.put("typeName", ALARM_TYPE_NAMES.getOrDefault(key, key));
        }
        return Result.success(dbResult);
    }

    /**
     * 最新告警列表
     *
     * <p>返回最近 N 条告警记录，含设备名称</p>
     */
    @GetMapping("/latest-alarm")
    public Result<List<AlarmRecord>> latestAlarm(
            @RequestParam(defaultValue = "10") int limit) {
        List<AlarmRecord> list = alarmRecordMapper.latestAlarmList(limit);
        return Result.success(list);
    }

    /**
     * 工单快照统计
     *
     * <p>返回今日新增 + 各状态分布</p>
     */
    @GetMapping("/workorder/stats")
    public Result<Map<String, Object>> workOrderStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayCount", workOrderMapper.countToday());

        List<Map<String, Object>> statusDist = workOrderMapper.countByStatus();
        Map<Integer, Long> statusMap = new HashMap<>();
        for (Map<String, Object> row : statusDist) {
            Number statusNum = (Number) row.get("status");
            Number countNum = (Number) row.get("count");
            statusMap.put(statusNum.intValue(), countNum.longValue());
        }
        result.put("pending", statusMap.getOrDefault(0, 0L));
        result.put("processing", statusMap.getOrDefault(1, 0L));
        result.put("completed", statusMap.getOrDefault(2, 0L));
        result.put("verified", statusMap.getOrDefault(3, 0L));
        return Result.success(result);
    }
}
