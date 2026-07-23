package com.ccb.lighting.module.environment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.environment.entity.EnvSensorData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境传感器数据 Service 接口
 *
 * <p>方法清单：
 * - pageList：分页查询环境数据
 * - add：新增一条环境数据（设备上报时调用）
 * - latest：查询某灯杆的最新环境数据
 * - trend：查询某灯杆环境数据趋势</p>
 */
public interface EnvSensorDataService {

    /**
     * 分页查询环境数据
     *
     * @param query 分页参数
     * @return 分页对象
     */
    IPage<EnvSensorData> pageList(PageQuery query);

    /**
     * 新增环境数据
     *
     * @param data 环境数据
     */
    void add(EnvSensorData data);

    /**
     * 查询某灯杆的最新环境数据
     * 用于前端首页/灯杆详情页展示实时数据
     *
     * @param poleId 灯杆 ID
     * @return 最新一条环境数据，无数据返回 null
     */
    EnvSensorData latest(Long poleId);

    /**
     * 查询某灯杆的环境数据趋势
     *
     * @param poleId    灯杆 ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 环境数据列表（按时间正序）
     */
    List<EnvSensorData> trend(Long poleId, LocalDateTime startTime, LocalDateTime endTime);
}
