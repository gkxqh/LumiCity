package com.ccb.lighting.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统菜单 Mapper 接口
 *
 * <p>继承 BaseMapper<SysMenu> 即获得菜单的单表 CRUD 能力。
 * 菜单树组装、按角色查权限等复杂逻辑通常放在 Service 层用 QueryWrapper 完成，
 * 因此此处暂不写自定义 SQL。</p>
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}
