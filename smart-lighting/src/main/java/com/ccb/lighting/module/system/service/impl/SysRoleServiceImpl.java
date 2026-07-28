package com.ccb.lighting.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.system.entity.SysRole;
import com.ccb.lighting.module.system.mapper.SysRoleMapper;
import com.ccb.lighting.module.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统角色 Service 实现类
 *
 * <p>角色 CRUD 由 BaseMapper 提供；与菜单的绑定关系通过 SysRoleMapper 的注解 SQL 维护。
 * assignMenus 采用“先删后插”整体重写，简单且避免增量更新的脏数据。</p>
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public List<SysRole> list(String roleName, Integer status) {//部分页查询角色列表
        return sysRoleMapper.selectList(buildWrapper(roleName, status));
    }

    @Override//分页查询角色列表
    public Page<SysRole> pageList(Integer current, Integer size, String roleName, Integer status) {
        return sysRoleMapper.selectPage(new Page<>(current, size), buildWrapper(roleName, status));
    }

    @Override//根据 id 查单个角色详情
    public SysRole getById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    @Override
    public void add(SysRole role) {//添加角色
        // 角色编码唯一性校验
        Long count = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role.getRoleCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS);
        }
        sysRoleMapper.insert(role);
    }

    @Override//修改角色
    public void update(SysRole role) {
        sysRoleMapper.updateById(role);
    }

    @Override
    public void delete(Long id) {
        // 逻辑删除角色本身
        sysRoleMapper.deleteById(id);
        // 清理角色-菜单绑定，避免留下悬空关联
        sysRoleMapper.deleteRoleMenus(id);
    }

    @Override//查某角色绑定的菜单 ID 列表
    public List<Long> getMenuIds(Long roleId) {
        return sysRoleMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override//给角色重新分配菜单
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 先删后插，整体重写
        sysRoleMapper.deleteRoleMenus(roleId);
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                sysRoleMapper.insertRoleMenu(roleId, menuId);
            }
        }
    }

    /** 构造查询条件：角色名模糊、状态等值 */
    private LambdaQueryWrapper<SysRole> buildWrapper(String roleName, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName);//如果角色名不为空，则模糊匹配
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);//状态=1则精确匹配
        }
        wrapper.orderByDesc(SysRole::getCreateTime);//最新的排最前
        return wrapper;
    }
}
