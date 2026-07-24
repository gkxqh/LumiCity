package com.ccb.lighting.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色 Mapper 接口
 *
 * <p>继承 BaseMapper<SysRole> 即获得角色的单表 CRUD 能力。
 * 角色与菜单是多对多（sys_role_menu），这里用注解写几条关联 SQL：
 * - selectMenuIdsByRoleId：查某角色绑定的菜单 ID 列表（用于回显勾选）
 * - deleteRoleMenus / insertRoleMenu：重写角色-菜单绑定关系（分配权限时用）</p>
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询某角色绑定的全部菜单 ID */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 删除某角色的全部菜单关联（重新分配前调用） */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenus(@Param("roleId") Long roleId);

    /** 给某角色绑定一个菜单 */
    @Insert("INSERT INTO sys_role_menu(role_id, menu_id, create_time, update_time) " +
            "VALUES(#{roleId}, #{menuId}, NOW(), NOW())")
    void insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
