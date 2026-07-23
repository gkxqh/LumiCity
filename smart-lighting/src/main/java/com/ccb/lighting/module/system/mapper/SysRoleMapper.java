package com.ccb.lighting.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统角色 Mapper 接口
 *
 * <p>继承 BaseMapper<SysRole> 即获得角色的单表 CRUD 能力，本学习蓝本暂不需要自定义 SQL。
 * 后续如需"根据菜单查角色""批量删除角色"等，可在此追加 @Select / @Delete 方法或对应 XML。</p>
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
