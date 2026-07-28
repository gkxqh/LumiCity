package com.ccb.lighting.module.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.device.dto.PoleQueryDTO;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.service.DevPoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 灯杆 Controller
 *
 * <p>路径前缀 /device/pole。pole_name 和 address 由服务层自动拼接，
 * 前端新增/修改时无需传这两个字段，传 regionId/road/number 即可。</p>
 */
@RestController
@RequestMapping("/device/pole")
@RequiredArgsConstructor
public class DevPoleController {

    private final DevPoleService devPoleService;

    /**
     * 分页查询灯杆列表
     * GET /device/pole/page?current=1&size=10&poleName=人民路&status=1&regionId=2&road=科华北路
     */
    @GetMapping("/page")
    public Result<IPage<DevPole>> page(PoleQueryDTO query) {
        IPage<DevPole> page = devPoleService.pageList(query);
        return Result.success(page);
    }

    /** 查询全部灯杆（不分页，给下拉框用） */
    @GetMapping("/list")
    public Result<List<DevPole>> list() {
        List<DevPole> list = devPoleService.list();
        return Result.success(list);
    }

    /** 根据 id 查询灯杆详情 */
    @GetMapping("/{id}")
    public Result<DevPole> getById(@PathVariable Long id) {
        DevPole pole = devPoleService.getById(id);
        return Result.success(pole);
    }

    /**
     * 新增灯杆
     *
     * <p>请求体示例：{"poleCode":"P-2024-001","regionId":2,"road":"科华北路","number":"88号","lng":104.05,"lat":30.63,"height":8,"status":1}
     * poleName 和 address 由服务层自动填充，前端不需传。</p>
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DevPole pole) {
        devPoleService.add(pole);
        return Result.success();
    }

    /**
     * 修改灯杆
     *
     * <p>仅需传需要变更的字段 + id。regionId/road/number 任一变化时会自动重新生成 poleName 和 address。</p>
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody DevPole pole) {
        devPoleService.update(pole);
        return Result.success();
    }

    /** 根据 id 删除灯杆 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        devPoleService.delete(id);
        return Result.success();
    }
}
