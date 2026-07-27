package com.ccb.lighting.module.publish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.common.SecurityContext;
import com.ccb.lighting.module.publish.dto.LedProgramQueryDTO;
import com.ccb.lighting.module.publish.entity.LedProgram;
import com.ccb.lighting.module.publish.entity.LedPublishLog;
import com.ccb.lighting.module.publish.mapper.LedProgramMapper;
import com.ccb.lighting.module.publish.service.LedProgramService;
import com.ccb.lighting.module.publish.service.LedPublishLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * LED 节目 Service 实现类
 *
 * <p>关键实现：
 * - 新增：默认状态为 0 待发布
 * - 发布：状态改为已发布 + 写入发布时间 + 记录发布日志
 * - 发布日志用于演示时追溯发布历史，展示"已成功推送到屏幕"</p>
 */
@Service
@RequiredArgsConstructor
public class LedProgramServiceImpl implements LedProgramService {

    /** LED 节目 Mapper，构造器注入 */
    private final LedProgramMapper ledProgramMapper;

    /** 发布记录 Service，构造器注入 */
    private final LedPublishLogService ledPublishLogService;

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

    @Override
    public IPage<LedProgram> pageListByQuery(LedProgramQueryDTO query) {
        Page<LedProgram> page = new Page<>(query.getCurrent(), query.getSize());
        return ledProgramMapper.pageListByQuery(page, query);
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
     *
     * <p>将节目状态改为已发布，记录发布时间，并写入发布日志。
     * 发布日志包含操作人、时间、内容预览等信息，
     * 供前端发布历史弹窗展示，模拟"推送成功"的效果。</p>
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

        // 1. 更新节目状态 + 发布时间
        LocalDateTime now = LocalDateTime.now();
        program.setStatus(1);
        program.setPublishTime(now);
        ledProgramMapper.updateById(program);

        // 2. 写入发布记录，追溯发布历史
        LedPublishLog log = new LedPublishLog();
        log.setProgramId(program.getId());
        log.setProgramName(program.getProgramName());
        log.setMediaType(program.getMediaType());

        // 内容预览：文本截取前 200 字，图片/视频取文件名
        String preview = program.getContent();
        if (preview != null) {
            if ("TEXT".equals(program.getMediaType())) {
                preview = preview.length() > 200 ? preview.substring(0, 200) + "..." : preview;
            } else {
                // IMAGE/VIDEO 取文件名最后一段
                int idx = preview.lastIndexOf('/');
                if (idx >= 0) {
                    preview = preview.substring(idx + 1);
                }
            }
        }
        log.setContentPreview(preview);

        // 从 SecurityContext 取当前用户（JWT 拦截器写入）
        log.setOperatorId(SecurityContext.getUserId());
        // 当前线程有 SecurityInfo 但无 username 字段直接存储，取用户ID的字符串表示
        log.setOperator(String.valueOf(SecurityContext.getUserId()));

        log.setPublishTime(now);
        log.setPushStatus("SUCCESS");
        log.setPushMessage("节目已推送到 LED 屏幕，正在播放中");
        ledPublishLogService.add(log);
    }
}
