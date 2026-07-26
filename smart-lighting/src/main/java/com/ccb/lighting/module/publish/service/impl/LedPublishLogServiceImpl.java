package com.ccb.lighting.module.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.publish.entity.LedPublishLog;
import com.ccb.lighting.module.publish.mapper.LedPublishLogMapper;
import com.ccb.lighting.module.publish.service.LedPublishLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * LED 节目发布记录 Service 实现类
 *
 * <p>按节目ID分页查询发布历史，按发布时间倒序排列。</p>
 */
@Service
@RequiredArgsConstructor
public class LedPublishLogServiceImpl implements LedPublishLogService {

    private final LedPublishLogMapper ledPublishLogMapper;

    @Override
    public void add(LedPublishLog log) {
        ledPublishLogMapper.insert(log);
    }

    @Override
    public IPage<LedPublishLog> pageList(PageQuery query, Long programId) {
        LambdaQueryWrapper<LedPublishLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LedPublishLog::getProgramId, programId);
        wrapper.orderByDesc(LedPublishLog::getPublishTime);
        return ledPublishLogMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }
}
