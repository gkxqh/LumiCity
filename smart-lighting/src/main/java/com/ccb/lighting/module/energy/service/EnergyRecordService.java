package com.ccb.lighting.module.energy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.energy.entity.EnergyRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 能耗记录 Service 接口
 *
 * <p>方法清单：
 * - pageList：分页查询能耗记录（支持按设备ID、灯杆、时间范围筛选）
 * - getById：根据 id 查单条记录
 * - add：新增一条能耗记录（设备上报时调用）
 * - trend：查询某设备在时间范围内的能耗趋势（折线图数据）
 * - statistics：能耗统计汇总（总用电量、平均功率等）</p>
 */
public interface EnergyRecordService {

    /**
     * 分页查询能耗记录
     *
     * @param query 分页参数
     * @return 分页对象
     */
    IPage<EnergyRecord> pageList(PageQuery query);

    /**
     * 根据 id 查询能耗记录
     *
     * @param id 记录 ID
     * @return 能耗记录实体
     */
    EnergyRecord getById(Long id);

    /**
     * 新增能耗记录
     *
     * @param record 能耗数据
     */
    void add(EnergyRecord record);

    /**
     * 查询设备能耗趋势
     *
     * <p>典型用法：前端折线图 X 轴为时间、Y 轴为用电量/功率。
     * 按时间正序返回，便于前端直接绘制。</p>
     *
     * @param deviceId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 能耗记录列表（按时间正序）
     */
    List<EnergyRecord> trend(String deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 能耗统计汇总
     *
     * <p>返回总用电量、平均功率、记录数等汇总指标，用于数据大盘展示。</p>
     *
     * @return 统计结果 Map
     */
    Map<String, Object> statistics();
}
