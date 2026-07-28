package com.ccb.lighting.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.system.entity.SysMenu;
import com.ccb.lighting.module.system.mapper.SysMenuMapper;
import com.ccb.lighting.module.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统菜单 Service 实现类
 *
 * <p>菜单是树形结构（parent_id 指向父节点，顶层为 0）。
 * tree() 用“一次查出全部 + 内存分组”的方式组装树，比递归查库更高效，适合菜单量级小的场景。</p>
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenu> list() {
        // 按排序号升序，前端展示/下拉选择顺序稳定
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getOrderNum));
    }

    @Override
    public List<SysMenu> tree() {
        List<SysMenu> all = list();
        // 以 parentId 为 key 分组
        Map<Long, List<SysMenu>> childMap = new HashMap<>();
        for (SysMenu m : all) {
            Long pid = m.getParentId() == null ? 0L : m.getParentId();
            childMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(m);
        }
        // 顶层节点（parentId = 0）作为树根
        List<SysMenu> roots = childMap.getOrDefault(0L, new ArrayList<>());
        buildChildren(roots, childMap);//递归把子节点挂到父节点的 children 上
        return roots;
    }

    @Override
    public SysMenu getById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    @Override
    public void add(SysMenu menu) {
        sysMenuMapper.insert(menu);
    }

    @Override
    public void update(SysMenu menu) {
        sysMenuMapper.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        // 存在子菜单则拒绝删除，避免产生孤儿节点
        Long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "该菜单下存在子项，请先删除子项");
        }
        sysMenuMapper.deleteById(id);
    }

    /** 递归把子节点挂到父节点的 children 上 */
    private void buildChildren(List<SysMenu> nodes, Map<Long, List<SysMenu>> childMap) {
        for (SysMenu node : nodes) {
            List<SysMenu> kids = childMap.get(node.getId());
            if (kids != null && !kids.isEmpty()) {
                node.setChildren(kids);
                buildChildren(kids, childMap);
            }
        }
    }
}
