package com.ccb.lighting.module.energy.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.energy.entity.EnergyRecord;
import com.ccb.lighting.module.energy.mapper.EnergyRecordMapper;
import com.ccb.lighting.module.energy.service.EnergyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能耗记录 Service 实现类
 *
 * <p>核心实现要点：
 * - 分页查询：支持按设备ID、灯杆ID、时间范围组合筛选
 * - 趋势查询：selectList + 时间范围条件，按记录时间正序返回
 * - 统计汇总：用 selectCount 获取记录数，可扩展为 SQL 聚合查询</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyRecordServiceImpl implements EnergyRecordService {

    /** 能耗记录 Mapper，构造器注入 */
    private final EnergyRecordMapper energyRecordMapper;

    /**
     * 分页查询能耗记录
     * 默认按记录时间倒序，最新数据排前面
     */
    @Override
    public IPage<EnergyRecord> pageList(PageQuery query) {
        LambdaQueryWrapper<EnergyRecord> wrapper = new LambdaQueryWrapper<>();
        // 这里若需更精细筛选，可定义 EnergyQueryDTO 扩展字段
        wrapper.orderByDesc(EnergyRecord::getRecordTime);
        return energyRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询能耗记录
     */
    @Override
    public EnergyRecord getById(Long id) {
        return energyRecordMapper.selectById(id);
    }

    /**
     * 新增能耗记录
     * 设备定时上报时调用此接口
     */
    @Override
    public void add(EnergyRecord record) {
        // 若未传记录时间，默认当前时间
        if (record.getRecordTime() == null) {
            record.setRecordTime(LocalDateTime.now());
        }
        energyRecordMapper.insert(record);
    }

    /**
     * 查询设备能耗趋势
     * 按记录时间正序返回，前端折线图直接绘制
     */
    @Override
    public List<EnergyRecord> trend(String deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<EnergyRecord> wrapper = new LambdaQueryWrapper<>();
        // 设备ID精确匹配
        if (StringUtils.hasText(deviceId)) {
            wrapper.eq(EnergyRecord::getDeviceId, deviceId);
        }
        // 时间范围：record_time >= startTime AND record_time <= endTime
        if (startTime != null) {
            wrapper.ge(EnergyRecord::getRecordTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(EnergyRecord::getRecordTime, endTime);
        }
        // 按时间正序，折线图从左到右时间递增
        wrapper.orderByAsc(EnergyRecord::getRecordTime);
        return energyRecordMapper.selectList(wrapper);
    }

    /**
     * 能耗统计汇总
     * 简化实现：返回总记录数与采样说明
     * 真实场景应写 SQL：SELECT SUM(consumption), AVG(power), COUNT(*) FROM energy_record
     */
    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new HashMap<>();
        Long total = energyRecordMapper.selectCount(null);
        result.put("totalRecords", total);
        result.put("description", "总记录数，可扩展为 SUM(consumption) 等聚合统计");
        return result;
    }

    /**
     * 导出能耗报表
     * 根据条件筛选能耗记录，使用 EasyExcel 导出为 Excel 文件
     * 直接使用 EnergyRecord 实体（已添加 @ExcelProperty 注解），无需转换
     */
    @Override
    public void exportReport(OutputStream outputStream, String deviceId, 
                            LocalDateTime startTime, LocalDateTime endTime) {
        // 构建查询条件
        LambdaQueryWrapper<EnergyRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deviceId)) {
            wrapper.eq(EnergyRecord::getDeviceId, deviceId);
        }
        if (startTime != null) {
            wrapper.ge(EnergyRecord::getRecordTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(EnergyRecord::getRecordTime, endTime);
        }
        wrapper.orderByAsc(EnergyRecord::getRecordTime);

        // 查询数据
        List<EnergyRecord> records = energyRecordMapper.selectList(wrapper);

        try {
            // 直接使用 EnergyRecord 实体，@ExcelProperty 注解定义了 Excel 列标题
            EasyExcel.write(outputStream, EnergyRecord.class)
                    .sheet("能耗报表")
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .doWrite(records);
        } catch (Exception e) {
            log.error("能耗报表导出失败", e);
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }
}
