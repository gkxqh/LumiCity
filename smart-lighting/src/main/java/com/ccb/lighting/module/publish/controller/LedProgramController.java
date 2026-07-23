package com.ccb.lighting.module.publish.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.PageQuery;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.publish.entity.LedProgram;
import com.ccb.lighting.module.publish.service.LedProgramService;
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
 * LED 节目 Controller
 *
 * <p>路径前缀 /publish/program，提供 LED 屏节目的 CRUD + 发布接口。</p>
 *
 * <p>接口列表：
 * - GET    /publish/program/page        分页查询
 * - GET    /publish/program/{id}        查详情
 * - POST   /publish/program             新增
 * - PUT    /publish/program             修改
 * - DELETE /publish/program/{id}         删除
 * - PUT    /publish/program/{id}/publish 发布节目</p>
 */
@RestController
@RequestMapping("/publish/program")
@RequiredArgsConstructor
public class LedProgramController {

    /** LED 节目 Service，构造器注入 */
    private final LedProgramService ledProgramService;

    /**
     * 分页查询节目列表
     *
     * @param query 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<LedProgram>> page(PageQuery query) {
        return Result.success(ledProgramService.pageList(query));
    }

    /**
     * 根据 id 查询节目详情
     *
     * @param id 节目 ID
     * @return 节目信息
     */
    @GetMapping("/{id}")
    public Result<LedProgram> getById(@PathVariable Long id) {
        return Result.success(ledProgramService.getById(id));
    }

    /**
     * 新增节目
     *
     * @param program 节目信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody LedProgram program) {
        ledProgramService.add(program);
        return Result.success();
    }

    /**
     * 修改节目
     *
     * @param program 节目信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody LedProgram program) {
        ledProgramService.update(program);
        return Result.success();
    }

    /**
     * 根据 id 删除节目
     *
     * @param id 节目 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ledProgramService.delete(id);
        return Result.success();
    }

    /**
     * 发布节目
     * 将节目状态改为已发布，并下发到 LED 屏幕
     *
     * @param id 节目 ID
     * @return 操作结果
     */
    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        ledProgramService.publish(id);
        return Result.success();
    }
}
