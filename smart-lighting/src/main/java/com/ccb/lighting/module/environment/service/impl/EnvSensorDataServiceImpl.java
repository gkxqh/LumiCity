package com.ccb.lighting.module.environment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.environment.entity.EnvSensorData;
import com.ccb.lighting.module.environment.mapper.EnvSensorDataMapper;
import com.ccb.lighting.module.environment.service.EnvSensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境传感器数据 Service 实现类
 *
 * <p>关键实现：
 * - pageList：按记录时间倒序分页
 * - latest：按 poleId 过滤 + 按记录时间倒序取第一条
 * - trend：按 poleId + 时间范围查询，时间正序返回</p>
 */
@Service
@RequiredArgsConstructor
public class EnvSensorDataServiceImpl implements EnvSensorDataService {

    /** 环境传感器数据 Mapper，构造器注入 */
    private final EnvSensorDataMapper envSensorDataMapper;

    /**
     * 分页查询环境数据
     * 按记录时间倒序，最新数据排前面
     */
    @Override
    public IPage<EnvSensorData> pageList(PageQuery query) {
        LambdaQueryWrapper<EnvSensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EnvSensorData::getRecordTime);
        return envSensorDataMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 新增环境数据
     * 若未传记录时间，默认当前时间
     */
    @Override
    public void add(EnvSensorData data) {
        if (data.getRecordTime() == null) {
            data.setRecordTime(LocalDateTime.now());
        }
        envSensorDataMapper.insert(data);
    }

    /**
     * 查询某灯杆的最新环境数据
     * 按 recordTime 倒序取第一条
     */
    @Override
    public EnvSensorData latest(Long poleId) {
        return envSensorDataMapper.selectOne(
                new LambdaQueryWrapper<EnvSensorData>()
                        .eq(EnvSensorData::getPoleId, poleId)
                        .orderByDesc(EnvSensorData::getRecordTime)
                        .last("LIMIT 1")
        );
    }

    /**
     * 查询环境数据趋势
     * 按记录时间正序，前端折线图时间轴从左到右递增
     */
    @Override
    public List<EnvSensorData> trend(Long poleId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<EnvSensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnvSensorData::getPoleId, poleId);
        if (startTime != null) {
            wrapper.ge(EnvSensorData::getRecordTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(EnvSensorData::getRecordTime, endTime);
        }
        wrapper.orderByAsc(EnvSensorData::getRecordTime);
        return envSensorDataMapper.selectList(wrapper);
    }
}
