package com.ccb.lighting.module.publish.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.publish.entity.LedPublishLog;

/**
 * LED 节目发布记录 Service 接口
 *
 * <p>提供发布记录的写入和分页查询，供 publish 操作和前端发布历史使用。</p>
 */
public interface LedPublishLogService {

    /**
     * 新增发布记录
     *
     * @param log 发布记录
     */
    void add(LedPublishLog log);

    /**
     * 分页查询某节目的发布历史
     *
     * @param query     分页参数
     * @param programId 节目ID
     * @return 分页数据
     */
    IPage<LedPublishLog> pageList(PageQuery query, Long programId);
}
