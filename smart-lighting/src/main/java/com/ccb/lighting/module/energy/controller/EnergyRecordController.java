package com.ccb.lighting.module.energy.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import com.ccb.lighting.module.energy.service.EnergyRecordService;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 能耗记录 Controller
 *
 * <p>路径前缀 /energy，提供能耗数据的查询、趋势、统计接口。
 * 能耗数据由设备主动上报入库，前端主要负责展示与分析。</p>
 *
 * <p>接口列表：
 * - GET  /energy/page        分页查询能耗记录
 * - GET  /energy/{id}        查单条记录详情
 * - POST /energy              新增能耗记录（设备上报）
 * - GET  /energy/trend       查询设备能耗趋势（折线图）
 * - GET  /energy/statistics  能耗统计汇总（数据大盘用）</p>
 */
@RestController
@RequestMapping("/energy")
@RequiredArgsConstructor
public class EnergyRecordController {

    /** 能耗记录 Service，构造器注入 */
    private final EnergyRecordService energyRecordService;

    /**
     * 分页查询能耗记录
     *
     * @param query 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<EnergyRecord>> page(PageQuery query) {
        return Result.success(energyRecordService.pageList(query));
    }

    /**
     * 根据 id 查询能耗记录详情
     *
     * @param id 记录 ID
     * @return 能耗记录
     */
    @GetMapping("/{id}")
    public Result<EnergyRecord> getById(@PathVariable Long id) {
        return Result.success(energyRecordService.getById(id));
    }

    /**
     * 新增能耗记录
     * 通常由设备上报接口调用，而非人工操作
     *
     * @param record 能耗数据
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody EnergyRecord record) {
        energyRecordService.add(record);
        return Result.success();
    }

    /**
     * 查询设备能耗趋势
     *
     * <p>请求示例：GET /energy/trend?deviceId=D-001&startTime=2026-07-01T00:00:00&endTime=2026-07-22T23:59:59
     * 返回该设备在时间范围内的所有能耗记录，前端按 recordTime 绘制折线图。</p>
     *
     * <p>@DateTimeFormat：告诉 Spring 把字符串参数解析为 LocalDateTime，
     * 格式 ISO（yyyy-MM-ddTHH:mm:ss）或自定义 pattern。</p>
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 能耗记录列表
     */
    @GetMapping("/trend")
    public Result<List<EnergyRecord>> trend(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(energyRecordService.trend(deviceId, startTime, endTime));
    }

    /**
     * 能耗统计汇总
     * 返回总记录数等聚合指标，用于数据大盘
     *
     * @return 统计结果 Map
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(energyRecordService.statistics());
    }

    /**
     * 导出能耗报表
     *
     * <p>支持按设备ID和时间范围筛选导出，默认导出全部。
     * 前端通过 GET 请求下载 Excel 文件。</p>
     *
     * @param deviceId  设备ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param response  HTTP 响应对象
     */
    @GetMapping("/export")
    public void exportReport(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            HttpServletResponse response) throws IOException {
        
        // 设置响应头
        String filename = "能耗报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        // 调用 Service 导出
        energyRecordService.exportReport(response.getOutputStream(), deviceId, startTime, endTime);
    }
}
