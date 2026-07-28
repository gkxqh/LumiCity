package com.ccb.lighting.module.system.controller;

import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.system.entity.SysMenu;
import com.ccb.lighting.module.system.service.SysMenuService;
import com.ccb.lighting.security.RequiresPerms;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统菜单 Controller
 *
 * <p>路径前缀 /system/menu，提供菜单的 CRUD 与树形查询。
 * 写接口均标注 @RequiresPerms，由 JwtInterceptor 做接口级鉴权。</p>
 */
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @RequiresPerms("system:menu:list")
    @GetMapping("/list")//查询所有菜单（没用过）
    public Result<List<SysMenu>> list() {
        return Result.success(sysMenuService.list());
    }

    @RequiresPerms("system:menu:list")
    @GetMapping("/tree")//查询菜单树（menu.vue（菜单管理页）、role.vue（角色分配权限时加载菜单树）用了）
    public Result<List<SysMenu>> tree() {
        return Result.success(sysMenuService.tree());
    }

    @RequiresPerms("system:menu:list")
    @GetMapping("/{id}")//根据ID查询菜单详情（没用过）
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(sysMenuService.getById(id));
    }

    @RequiresPerms("system:menu:add")
    @PostMapping//添加菜单（menu.vue的addmenu）
    public Result<Void> add(@RequestBody SysMenu menu) {
        sysMenuService.add(menu);
        return Result.success();
    }

    @RequiresPerms("system:menu:edit")
    @PutMapping//修改菜单（menu.vue的updatemenu）
    public Result<Void> update(@RequestBody SysMenu menu) {
        sysMenuService.update(menu);
        return Result.success();
    }

    @RequiresPerms("system:menu:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {//删除菜单（menu.vue的deletemenu）
        sysMenuService.delete(id);
        return Result.success();
    }
}
