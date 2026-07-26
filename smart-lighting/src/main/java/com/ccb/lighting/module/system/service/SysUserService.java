package com.ccb.lighting.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Service 接口
 *
 * <p>三层架构中的"业务层"接口。Controller 只依赖此接口，不依赖实现类，
 * 面向接口编程，便于切换实现、做单元测试 mock。</p>
 *
 * <p>方法说明：
 * - pageList：分页 + 条件查询用户列表
 * - getById：根据 id 查单个用户（详情）
 * - add：新增用户（含用户名查重、密码加密）
 * - update：修改用户
 * - delete：根据 id 删除用户（逻辑删除）
 * - findByUsername：根据用户名查用户（登录时用）</p>
 */
public interface SysUserService {

    /**
     * 分页查询用户列表
     *
     * @param current 当前页码（从 1 开始）
     * @param size    每页条数
     * @param query   查询条件（用 SysUser 实体承载，非 null 的字段作为过滤条件，如 username 模糊匹配）
     * @return MyBatis-Plus 的 Page 对象，含 records（数据）、total（总数）、current、size 等
     */
    Page<SysUser> pageList(Integer current, Integer size, SysUser query);

    /**
     * 根据 id 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户实体，不存在返回 null
     */
    SysUser getById(Long id);

    /**
     * 新增用户
     * 内部逻辑：校验用户名是否重复 → 密码 BCrypt 加密 → 入库
     *
     * @param user 前端传入的用户信息（密码为明文，方法内加密）
     */
    void add(SysUser user);

    /**
     * 修改用户
     * 注意：本方法不修改密码，改密码应单独走重置流程
     *
     * @param user 待更新的用户信息（需含 id）
     */
    void update(SysUser user);

    /**
     * 根据 id 删除用户（BaseEntity 的 @TableLogic 会让 MyBatis-Plus 自动改为逻辑删除）
     *
     * @param id 用户 ID
     */
    void delete(Long id);
    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    SysUser findByUsername(String username);

    /**
     * 查询某用户拥有的角色 ID 列表
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表（可能为空）
     */
    List<Long> getUserRoleIds(Long userId);

    /**
     * 给用户重新分配角色（整体重写 sys_user_role）
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表（可空，表示清空角色）
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 根据角色代码查询用户列表
     */
    List<SysUser> listByRoleCode(String roleCode);
}
