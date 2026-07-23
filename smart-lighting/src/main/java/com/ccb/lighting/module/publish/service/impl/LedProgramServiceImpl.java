package com.ccb.lighting.module.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.publish.entity.LedProgram;
import com.ccb.lighting.module.publish.mapper.LedProgramMapper;
import com.ccb.lighting.module.publish.service.LedProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * LED 节目 Service 实现类
 *
 * <p>关键实现：
 * - 新增：默认状态为 0 待发布
 * - 发布：状态从 0 改为 1，真实场景还要下发到屏幕设备</p>
 */
@Service
@RequiredArgsConstructor
public class LedProgramServiceImpl implements LedProgramService {

    /** LED 节目 Mapper，构造器注入 */
    private final LedProgramMapper ledProgramMapper;

    /**
     * 分页查询节目列表
     * 按创建时间倒序
     */
    @Override
    public IPage<LedProgram> pageList(PageQuery query) {
        LambdaQueryWrapper<LedProgram> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LedProgram::getCreateTime);
        return ledProgramMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询节目
     */
    @Override
    public LedProgram getById(Long id) {
        LedProgram program = ledProgramMapper.selectById(id);
        if (program == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return program;
    }

    /**
     * 新增节目
     * 默认状态为 0 待发布
     */
    @Override
    public void add(LedProgram program) {
        if (program.getStatus() == null) {
            program.setStatus(0);
        }
        // 默认播放模式 LOOP 循环
        if (program.getPlayMode() == null) {
            program.setPlayMode("LOOP");
        }
        ledProgramMapper.insert(program);
    }

    /**
     * 修改节目
     */
    @Override
    public void update(LedProgram program) {
        if (ledProgramMapper.selectById(program.getId()) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        ledProgramMapper.updateById(program);
    }

    /**
     * 删除节目
     */
    @Override
    public void delete(Long id) {
        ledProgramMapper.deleteById(id);
    }

    /**
     * 发布节目
     * 将状态从 0 待发布 改为 1 已发布
     * TODO: 真实场景通过 MQTT 下发节目内容到 LED 屏，并等待屏幕 ACK
     */
    @Override
    public void publish(Long id) {
        LedProgram program = ledProgramMapper.selectById(id);
        if (program == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 状态校验：仅待发布状态可发布
        if (program.getStatus() != null && program.getStatus() == 1) {
            throw new BusinessException("节目已发布，无需重复操作");
        }
        program.setStatus(1);
        ledProgramMapper.updateById(program);
    }
}
