package com.ccb.lighting.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.service.SysUserService;
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
 * 系统用户 Controller
 *
 * <p>路径前缀 /system/user，提供用户管理的增删改查（CRUD）接口。
 * 这些接口都需要登录后才能访问（由 JwtInterceptor 拦截），且按 @RequiresPerms 做接口级鉴权。</p>
 *
 * <p>RESTful 风格约定：
 * - GET    /system/user/page    分页查询（查询用 GET，参数走 URL）
 * - GET    /system/user/{id}    查详情
 * - POST   /system/user         新增（请求体带数据）
 * - PUT    /system/user         修改
 * - DELETE /system/user/{id}    删除
 * - GET    /system/user/{id}/roles   查用户拥有的角色 ID 列表
 * - PUT    /system/user/{id}/roles   给用户分配角色（请求体为角色 ID 列表）</p>
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    /** 用户 Service，构造器注入 */
    private final SysUserService sysUserService;

    @RequiresPerms("system:user:list")
    @GetMapping("/page")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String username,
            String phone,
            Integer status) {
        SysUser query = new SysUser();
        query.setUsername(username);
        query.setPhone(phone);
        query.setStatus(status);
        Page<SysUser> page = sysUserService.pageList(current, size, query);
        return Result.success(page);
    }

    @RequiresPerms("system:user:list")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        return Result.success(user);
    }

    @RequiresPerms("system:user:add")
    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        sysUserService.add(user);
        return Result.success();
    }

    @RequiresPerms("system:user:edit")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        sysUserService.update(user);
        return Result.success();
    }

    @RequiresPerms("system:user:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }

    @RequiresPerms("system:user:list")
    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(sysUserService.getUserRoleIds(id));
    }

    @RequiresPerms("system:user:edit")
    @PutMapping("/{id}/roles")
    public Result<Void> assignUserRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        sysUserService.assignRoles(id, roleIds);
        return Result.success();
    }
}
