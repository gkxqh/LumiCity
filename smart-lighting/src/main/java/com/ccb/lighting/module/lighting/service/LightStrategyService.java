package com.ccb.lighting.module.lighting.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.module.lighting.entity.LightStrategy;

/**
 * 照明策略 Service 接口
 *
 * <p>面向接口编程：Controller 依赖此接口，不依赖实现类。
 * 方法定义遵循"业务语义"，而非简单 CRUD。</p>
 *
 * <p>方法清单：
 * - pageList：分页查询策略列表（支持按名称模糊、类型、状态筛选）
 * - getById：根据 id 查策略详情
 * - add：新增策略
 * - update：修改策略
 * - delete：删除策略（逻辑删除）</p>
 */
public interface LightStrategyService {

    /**
     * 分页查询策略列表
     *
     * @param query 分页参数（含 current/size，及可选筛选字段）
     * @return 分页对象
     */
    IPage<LightStrategy> pageList(PageQuery query);

    /**
     * 根据 id 查询策略详情
     *
     * @param id 策略 ID
     * @return 策略实体，不存在返回 null
     */
    LightStrategy getById(Long id);

    /**
     * 新增策略
     *
     * @param strategy 策略信息
     */
    void add(LightStrategy strategy);

    /**
     * 修改策略
     *
     * @param strategy 策略信息（含 id）
     */
    void update(LightStrategy strategy);

    /**
     * 根据 id 删除策略
     *
     * @param id 策略 ID
     */
    void delete(Long id);
}
