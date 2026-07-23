package com.ccb.lighting.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.service.SysUserService;
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

/**
 * 系统用户 Controller
 *
 * <p>路径前缀 /system/user，提供用户管理的增删改查（CRUD）接口。
 * 这些接口都需要登录后才能访问（由 JwtInterceptor 拦截，未在排除清单中）。</p>
 *
 * <p>RESTful 风格约定：
 * - GET    /system/user/page    分页查询（查询用 GET，参数走 URL）
 * - GET    /system/user/{id}    查详情
 * - POST   /system/user         新增（请求体带数据）
 * - PUT    /system/user         修改
 * - DELETE /system/user/{id}    删除</p>
 *
 * <p>Controller 层职责单一：接收参数 → 调 Service → 包装 Result 返回。
 * 不写业务逻辑，业务逻辑全在 Service 实现类里。</p>
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    /** 用户 Service，构造器注入 */
    private final SysUserService sysUserService;

    /**
     * 分页查询用户列表
     *
     * <p>请求示例：GET /system/user/page?current=1&size=10&username=admin
     * Spring 自动把 current、size 绑定到方法参数，username/phone/status 绑定到 SysUser 对象。
     * SysUser 作为查询条件载体，传给 Service 构造 QueryWrapper。</p>
     *
     * @param current  当前页
     * @param size     每页条数
     * @param username 用户名（模糊查询，可空）
     * @param phone    手机号（精确查询，可空）
     * @param status   状态（可空）
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String username,
            String phone,
            Integer status) {
        // 把查询条件塞进 SysUser 实体，交给 Service 处理
        SysUser query = new SysUser();
        query.setUsername(username);
        query.setPhone(phone);
        query.setStatus(status);
        Page<SysUser> page = sysUserService.pageList(current, size, query);
        return Result.success(page);
    }

    /**
     * 根据 id 查询用户详情
     *
     * @param id 用户 ID，@PathVariable 从 URL 路径取值
     * @return 用户信息（密码字段不清空，后台管理可用；如需对外建议清空）
     */
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        return Result.success(user);
    }

    /**
     * 新增用户
     *
     * <p>请求体示例：{"username":"test","password":"123456","nickname":"测试","phone":"13800000000"}
     * 密码传明文，Service 层会查重 + MD5 加密后再入库。</p>
     *
     * @param user 用户信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        sysUserService.add(user);
        return Result.success();
    }

    /**
     * 修改用户
     *
     * <p>请求体需带 id。密码字段不会被更新（Service 层已置空）。</p>
     *
     * @param user 用户信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        sysUserService.update(user);
        return Result.success();
    }

    /**
     * 根据 id 删除用户（逻辑删除）
     *
     * @param id 用户 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.success();
    }
}
