package com.ccb.lighting.module.publish.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.publish.dto.LedProgramQueryDTO;
import com.ccb.lighting.module.publish.entity.LedProgram;

/**
 * LED 节目 Service 接口
 *
 * <p>方法清单：
 * - pageList：分页查询节目列表
 * - getById：根据 id 查节目详情
 * - add：新增节目
 * - update：修改节目
 * - delete：删除节目
 * - publish：发布节目（更新状态为已发布，并下发到屏幕）</p>
 */
public interface LedProgramService {

    /**
     * 分页查询节目列表
     *
     * @param query 分页参数
     * @return 分页对象
     */
    IPage<LedProgram> pageList(PageQuery query);

    /**
     * 根据 id 查询节目详情
     *
     * @param id 节目 ID
     * @return 节目实体
     */
    LedProgram getById(Long id);

    IPage<LedProgram> pageListByQuery(LedProgramQueryDTO query);

    /**
     * 新增节目
     *
     * @param program 节目信息
     */
    void add(LedProgram program);

    /**
     * 修改节目
     *
     * @param program 节目信息（含 id）
     */
    void update(LedProgram program);

    /**
     * 根据 id 删除节目
     *
     * @param id 节目 ID
     */
    void delete(Long id);

    /**
     * 发布节目
     * 将状态从"待发布"改为"已发布"，并下发到 LED 屏幕
     *
     * @param id 节目 ID
     */
    void publish(Long id);
}
