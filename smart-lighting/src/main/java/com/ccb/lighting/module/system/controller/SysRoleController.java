package com.ccb.lighting.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.system.entity.SysRole;
import com.ccb.lighting.module.system.service.SysRoleService;
import com.ccb.lighting.security.RequiresPerms;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统角色 Controller
 *
 * <p>路径前缀 /system/role，提供角色管理与“角色-菜单”权限分配。
 * 每个写接口均标注 @RequiresPerms，由 JwtInterceptor 做接口级鉴权。</p>
 */
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @RequiresPerms("system:role:list")
    @GetMapping("/list")
    public Result<List<SysRole>> list(String roleName, Integer status) {
        return Result.success(sysRoleService.list(roleName, status));
    }//查询角色列表，不分页，用于下拉选择等

    @RequiresPerms("system:role:list")
    @GetMapping("/page")
    public Result<Page<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String roleName, Integer status) {
        return Result.success(sysRoleService.pageList(current, size, roleName, status));
    }//分页查询角色列表

    @RequiresPerms("system:role:list")
    @GetMapping("/{id}")//根据ID查询角色详情
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(sysRoleService.getById(id));
    }

    @RequiresPerms("system:role:add")
    @PostMapping
    public Result<Void> add(@RequestBody SysRole role) {
        sysRoleService.add(role);
        return Result.success();
    }//添加角色

    @RequiresPerms("system:role:edit")
    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        sysRoleService.update(role);
        return Result.success();
    }//修改角色

    @RequiresPerms("system:role:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return Result.success();
    }//删除角色

    @RequiresPerms("system:role:list")
    @GetMapping("/{id}/menus")//查询某角色绑定的菜单ID列表
    public Result<List<Long>> getMenus(@PathVariable Long id) {
        return Result.success(sysRoleService.getMenuIds(id));
    }

    @RequiresPerms("system:role:edit")
    @PutMapping("/{id}/menus")//给角色分配菜单权限
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        sysRoleService.assignMenus(id, menuIds);
        return Result.success();
    }
}
