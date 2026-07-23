package com.ccb.lighting.module.lighting.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.lighting.entity.LightStrategy;
import com.ccb.lighting.module.lighting.service.LightStrategyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 照明策略 Controller
 *
 * <p>路径前缀 /lighting/strategy，提供照明策略的 CRUD 接口。
 * 策略配置好后由调度器定时执行，实现自动开关灯与调光。</p>
 *
 * <p>RESTful 风格：
 * - GET    /lighting/strategy/page    分页查询
 * - GET    /lighting/strategy/{id}    查详情
 * - POST   /lighting/strategy         新增
 * - PUT    /lighting/strategy         修改
 * - DELETE /lighting/strategy/{id}    删除</p>
 */
@RestController
@RequestMapping("/lighting/strategy")
@RequiredArgsConstructor
public class LightStrategyController {

    /** 照明策略 Service，构造器注入 */
    private final LightStrategyService lightStrategyService;

    /**
     * 分页查询策略列表
     *
     * @param query 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<LightStrategy>> page(PageQuery query) {
        return Result.success(lightStrategyService.pageList(query));
    }

    /**
     * 根据 id 查询策略详情
     *
     * @param id 策略 ID
     * @return 策略信息
     */
    @GetMapping("/{id}")
    public Result<LightStrategy> getById(@PathVariable Long id) {
        return Result.success(lightStrategyService.getById(id));
    }

    /**
     * 新增策略
     *
     * @param strategy 策略信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody LightStrategy strategy) {
        lightStrategyService.add(strategy);
        return Result.success();
    }

    /**
     * 修改策略
     *
     * @param strategy 策略信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody LightStrategy strategy) {
        lightStrategyService.update(strategy);
        return Result.success();
    }

    /**
     * 根据 id 删除策略
     *
     * @param id 策略 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        lightStrategyService.delete(id);
        return Result.success();
    }
}
