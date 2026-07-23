package com.ccb.lighting.module.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.alarm.entity.AlarmRecord;
import com.ccb.lighting.module.alarm.mapper.AlarmRecordMapper;
import com.ccb.lighting.module.device.entity.DevDevice;
import com.ccb.lighting.module.device.mapper.DevDeviceMapper;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import com.ccb.lighting.module.energy.mapper.EnergyRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据大盘 Controller
 *
 * <p>路径前缀 /dashboard，提供首页数据大盘的聚合查询接口。
 * 大盘不建表，而是聚合多个模块的数据：设备、告警、能耗，做综合统计展示。</p>
 *
 * <p>设计要点：
 * - 大盘数据来自多个模块的 Mapper，体现"跨模块业务聚合"场景
 * - 一个 Controller 可注入多个 Mapper，但复杂聚合建议拆到独立 Service
 * - 当前为骨架实现，真实场景可加缓存（如 Redis）降低数据库压力</p>
 *
 * <p>接口列表：
 * - GET /dashboard/overview      核心指标汇总（设备总数、在线数、告警数、今日能耗）
 * - GET /dashboard/alarm/trend   近7天告警趋势（折线图）</p>
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    /** 告警记录 Mapper，用于统计告警数量 */
    private final AlarmRecordMapper alarmRecordMapper;

    /** 设备 Mapper，用于统计设备总数与在线数 */
    private final DevDeviceMapper devDeviceMapper;

    /** 能耗记录 Mapper，用于统计今日能耗 */
    private final EnergyRecordMapper energyRecordMapper;

    /**
     * 核心指标汇总
     *
     * <p>返回首页大盘展示的核心数据：
     * - deviceTotal：设备总数
     * - deviceOnline：在线设备数（status=1）
     * - alarmPending：未处理告警数（status=0）
     * - todayRecords：今日能耗记录数</p>
     *
     * <p>TODO: 真实场景 todayEnergy 今日总用电量应通过 SQL SUM(consumption) 聚合，
     * 当前用记录数简化展示，后续可扩展为独立 DashboardService 做复杂聚合。</p>
     *
     * @return 核心指标 Map
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
        // 3. 未处理告警数：status=0
        Long alarmPending = alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>().eq(AlarmRecord::getStatus, 0)
        );
        // 4. 今日能耗记录数：record_time 在今天
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayRecords = energyRecordMapper.selectCount(
                new LambdaQueryWrapper<EnergyRecord>().ge(EnergyRecord::getRecordTime, todayStart)
        );

        result.put("deviceTotal", deviceTotal);
        result.put("deviceOnline", deviceOnline);
        result.put("alarmPending", alarmPending);
        result.put("todayRecords", todayRecords);
        // 在线率，便于前端展示百分比
        if (deviceTotal != null && deviceTotal > 0) {
            result.put("onlineRate", String.format("%.2f", deviceOnline * 100.0 / deviceTotal) + "%");
        } else {
            result.put("onlineRate", "0%");
        }
        return Result.success(result);
    }

    /**
     * 近7天告警趋势
     *
     * <p>返回近7天每天告警数量，用于前端折线图展示。
     * 当前为骨架实现返回模拟数据，真实场景应按日期分组聚合查询：
     * SELECT DATE(alarm_time), COUNT(*) FROM alarm_record
     * WHERE alarm_time >= 7天前 GROUP BY DATE(alarm_time)</p>
     *
     * @return 近7天告警趋势数据
     */
    @GetMapping("/alarm/trend")
    public Result<List<Map<String, Object>>> alarmTrend() {
        List<Map<String, Object>> result = new ArrayList<>();
        // 模拟近7天数据：日期 + 数量
        // TODO: 真实场景写 SQL 按日期 GROUP BY 聚合，这里返回空骨架便于前端联调
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", today.minusDays(i).toString());
            day.put("count", 0);
            result.add(day);
        }
        return Result.success(result);
    }
}
