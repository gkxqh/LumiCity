package com.ccb.lighting.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.system.entity.SysRole;
import com.ccb.lighting.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper 接口
 *
 * <p>继承 BaseMapper<SysUser> 后，MyBatis-Plus 自动提供单表的 CRUD 方法：
 * insert / deleteById / updateById / selectById / selectList 等，无需写 SQL。</p>
 *
 * <p>@Mapper 作用：让 Spring 容器扫描并生成代理实现类，可在 Service 里直接注入使用。
 * （项目若用了 @MapperScan 扫描包，可省略 @Mapper，但显式标注更直观，学习蓝本保留。）</p>
 *
 * <p>多表关联查询 BaseMapper 不支持，需自己写方法 + SQL。
 * 这里 selectUserRoleList 通过 @Select 注解写 SQL，查某用户的所有角色。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询某个用户拥有的所有角色列表（多表关联）
     *
     * <p>SQL 说明：用户与角色是多对多，通过中间表 sys_user_role 关联。
     * ur.user_id = #{userId} 限定指定用户，r.deleted = 0 排除已逻辑删除的角色。</p>
     *
     * @param userId 用户 ID
     * @return 角色列表，可能为空集合
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<SysRole> selectUserRoleList(@Param("userId") Long userId);
}
