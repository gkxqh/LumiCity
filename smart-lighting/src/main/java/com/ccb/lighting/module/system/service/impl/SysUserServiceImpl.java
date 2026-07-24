package com.ccb.lighting.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.system.entity.SysRole;
import com.ccb.lighting.module.system.entity.SysUser;
import com.ccb.lighting.module.system.mapper.SysUserMapper;
import com.ccb.lighting.module.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 系统用户 Service 实现类
 *
 * <p>注解说明：
 * - @Service：标记为业务层 Bean，交给 Spring 容器管理，Controller 才能注入
 * - @RequiredArgsConstructor：Lombok 生成带 final 字段的构造器，等价于
 *   public SysUserServiceImpl(SysUserMapper sysUserMapper){ this.sysUserMapper = sysUserMapper; }
 *   Spring 通过构造器把 sysUserMapper 注入进来，这就是"构造器注入"。</p>
 *
 * <p>为什么用构造器注入而不是 @Autowired 字段注入？
 * 1. 字段 final，注入后不可变，更安全
 * 2. 依赖关系一目了然，避免"循环依赖"隐患
 * 3. 不依赖 Spring 容器也能 db_tool.py 出对象做单元测试（传 mock）</p>
 *
 * <p>为什么注入 Mapper 而不是其他 Service？
 * 业务层操作数据库要经过 Mapper，所以注入 SysUserMapper。
 * MyBatis-Plus 的 BaseMapper 已提供单表 CRUD，直接调用即可。</p>
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    /** 用户 Mapper，构造器注入（final 必须在构造器里赋值，@RequiredArgsConstructor 帮我们做了） */
    private final SysUserMapper sysUserMapper;

    /**
     * 分页查询用户列表
     *
     * <p>LambdaQueryWrapper：MyBatis-Plus 的条件构造器，用 Lambda 方式写字段名，
     * 编译期检查字段名是否写错（比字符串 "username" 安全）。
     * like 是模糊查询：username like '%xxx%'；eq 是等值。</p>
     */
    @Override
    public Page<SysUser> pageList(Integer current, Integer size, SysUser query) {
        // 1. 构造查询条件：用户名模糊、手机号精确（仅当传入值时才拼接）
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            // StringUtils.hasText 判空，避免传 null 或空串时拼出 username like '%%' 把全部查出来
            if (org.springframework.util.StringUtils.hasText(query.getUsername())) {
                wrapper.like(SysUser::getUsername, query.getUsername());
            }
            if (org.springframework.util.StringUtils.hasText(query.getPhone())) {
                wrapper.eq(SysUser::getPhone, query.getPhone());
            }
            if (query.getStatus() != null) {
                wrapper.eq(SysUser::getStatus, query.getStatus());
            }
        }
        // 按创建时间倒序，最新用户排前面
        wrapper.orderByDesc(SysUser::getCreateTime);

        // 2. 执行分页查询：db_tool.py Page<>(current, size)，MyBatis-Plus 自动拼 limit
        return sysUserMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 根据 id 查询用户
     * selectById 是 BaseMapper 自带方法，内部自动过滤逻辑删除的数据
     */
    @Override
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    /**
     * 新增用户
     * 关键步骤：用户名查重 → 密码加密 → 入库
     */
    @Override
    public void add(SysUser user) {
        // 1. 用户名查重：用 LambdaQueryWrapper 查是否已存在同名用户
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())
        );
        if (count > 0) {
            // 抛业务异常，全局异常处理器会转成 Result 返回前端
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 2. 密码加密：用 Spring 自带的 DigestUtils 做 MD5（学习蓝本简化）
        //    生产环境强烈建议用 BCrypt（加盐、抗彩虹表），这里为不引入新依赖用 MD5
        user.setPassword(md5(user.getPassword()));

        // 3. 入库：insert 是 BaseMapper 自带方法
        //    createTime/updateTime/createBy 等由 MetaObjectHandler 自动填充，无需手动 set
        sysUserMapper.insert(user);
    }

    /**
     * 修改用户
     * updateById 按 id 更新非 null 字段。注意不更新密码（改密走单独流程）
     */
    @Override
    public void update(SysUser user) {
        // 防止通过修改接口篡改密码：强制置空 password，updateById 不会更新 null 字段
        user.setPassword(null);
        sysUserMapper.updateById(user);
    }

    /**
     * 删除用户
     * 因 BaseEntity.deleted 上有 @TableLogic，deleteById 实际执行 update set deleted=1（逻辑删除）
     */
    @Override
    public void delete(Long id) {
        sysUserMapper.deleteById(id);
    }

    /**
     * 根据用户名查用户（登录时用）
     * selectOne 期望最多一条，username 唯一所以安全
     */
    @Override
    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
    }

    /**
     * 查询用户拥有的角色 ID 列表
     * 复用多表关联方法 selectUserRoleList，再取 id 集合
     */
    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return sysUserMapper.selectUserRoleList(userId).stream()
                .map(SysRole::getId)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 给用户重新分配角色：先删后插，整体重写绑定关系
     */
    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        sysUserMapper.deleteUserRoles(userId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                sysUserMapper.insertUserRole(userId, roleId);
            }
        }
    }

    /**
     * MD5 加密工具方法（学习蓝本用，生产请换 BCrypt）
     * 用 Spring 的 DigestUtils，无需自己写 MessageDigest 样板代码
     */
    private String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }
}
