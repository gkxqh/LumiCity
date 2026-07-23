package com.ccb.lighting.module.environment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.environment.entity.EnvSensorData;
import com.ccb.lighting.module.environment.service.EnvSensorDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境监测 Controller
 *
 * <p>路径前缀 /env，提供环境数据的查询、上报、最新值、趋势接口。</p>
 *
 * <p>接口列表：
 * - GET  /env/page            分页查询环境数据
 * - GET  /env/latest/{poleId} 查询某灯杆最新环境数据
 * - GET  /env/trend           查询环境数据趋势
 * - POST /env                 新增环境数据（设备上报）</p>
 */
@RestController
@RequestMapping("/env")
@RequiredArgsConstructor
public class EnvSensorDataController {

    /** 环境传感器数据 Service，构造器注入 */
    private final EnvSensorDataService envSensorDataService;

    /**
     * 分页查询环境数据
     *
     * @param query 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<EnvSensorData>> page(PageQuery query) {
        return Result.success(envSensorDataService.pageList(query));
    }

    /**
     * 查询某灯杆的最新环境数据
     * 用于首页大屏、灯杆详情页展示实时数据
     *
     * @param poleId 灯杆 ID
     * @return 最新环境数据
     */
    @GetMapping("/latest/{poleId}")
    public Result<EnvSensorData> latest(@PathVariable Long poleId) {
        return Result.success(envSensorDataService.latest(poleId));
    }

    /**
     * 查询环境数据趋势
     *
     * <p>请求示例：GET /env/trend?poleId=100&startTime=2026-07-01T00:00:00&endTime=2026-07-22T23:59:59
     * 返回该灯杆在时间范围内的所有环境数据，前端按 recordTime 绘制折线图。</p>
     *
     * @param poleId    灯杆 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 环境数据列表
     */
    @GetMapping("/trend")
    public Result<List<EnvSensorData>> trend(
            @RequestParam Long poleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(envSensorDataService.trend(poleId, startTime, endTime));
    }

    /**
     * 新增环境数据
     * 通常由设备上报接口调用
     *
     * @param data 环境数据
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody EnvSensorData data) {
        envSensorDataService.add(data);
        return Result.success();
    }
}
