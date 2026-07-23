package com.ccb.lighting.module.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.lighting.entity.LightStrategy;
import com.ccb.lighting.module.lighting.mapper.LightStrategyMapper;
import com.ccb.lighting.module.lighting.service.LightStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 照明策略 Service 实现类
 *
 * <p>@Service 交给 Spring 容器管理；@RequiredArgsConstructor 通过构造器注入 Mapper。
 * 简单 CRUD 直接调用 BaseMapper 自带方法即可。</p>
 */
@Service
@RequiredArgsConstructor
public class LightStrategyServiceImpl implements LightStrategyService {

    /** 照明策略 Mapper，构造器注入 */
    private final LightStrategyMapper lightStrategyMapper;

    /**
     * 分页查询策略列表
     * 按创建时间倒序排列，最新策略排前面
     */
    @Override
    public IPage<LightStrategy> pageList(PageQuery query) {
        LambdaQueryWrapper<LightStrategy> wrapper = new LambdaQueryWrapper<>();
        // 按启用状态倒序：启用的策略排前面，便于运维查看生效中的策略
        wrapper.orderByDesc(LightStrategy::getEnabled)
                .orderByDesc(LightStrategy::getCreateTime);
        return lightStrategyMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询策略详情
     */
    @Override
    public LightStrategy getById(Long id) {
        LightStrategy strategy = lightStrategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return strategy;
    }

    /**
     * 新增策略
     * 校验策略名称是否重复后入库
     */
    @Override
    public void add(LightStrategy strategy) {
        // 策略名称查重：便于运维识别策略用途
        Long count = lightStrategyMapper.selectCount(
                new LambdaQueryWrapper<LightStrategy>()
                        .eq(LightStrategy::getStrategyName, strategy.getStrategyName())
        );
        if (count > 0) {
            throw new BusinessException("策略名称已存在，请更换");
        }
        // 默认启用
        if (strategy.getEnabled() == null) {
            strategy.setEnabled(1);
        }
        lightStrategyMapper.insert(strategy);
    }

    /**
     * 修改策略
     */
    @Override
    public void update(LightStrategy strategy) {
        // 校验策略是否存在
        if (lightStrategyMapper.selectById(strategy.getId()) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        lightStrategyMapper.updateById(strategy);
    }

    /**
     * 删除策略（逻辑删除）
     */
    @Override
    public void delete(Long id) {
        lightStrategyMapper.deleteById(id);
    }
}
