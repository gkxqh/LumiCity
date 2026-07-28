package com.ccb.lighting.module.device.controller;

import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.device.entity.Region;
import com.ccb.lighting.module.device.service.RegionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 区域管理 Controller
 *
 * <p>路径前缀 /device/region，提供区域 CRUD。
 * region 是扁平化的区级列表，不与灯杆的 road 字段耦合——区域+路的多层维度通过
 * dev_pole 的 region_id + road 两个字段分别存储。</p>
 */
@RestController
@RequestMapping("/device/region")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    /** 查询所有区域 */
    @GetMapping("/list")
    public Result<List<Region>> list() {
        return Result.success(regionService.list());
    }

    /** 根据 id 查询区域 */
    @GetMapping("/{id}")
    public Result<Region> getById(@PathVariable Long id) {
        return Result.success(regionService.getById(id));
    }

    /** 新增区域 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody Region region) {
        regionService.add(region);
        return Result.success();
    }

    /** 修改区域 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody Region region) {
        regionService.update(region);
        return Result.success();
    }

    /** 删除区域 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return Result.success();
    }
}
