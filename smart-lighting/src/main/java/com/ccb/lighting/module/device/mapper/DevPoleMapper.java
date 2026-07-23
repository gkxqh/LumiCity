package com.ccb.lighting.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.device.entity.DevPole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 灯杆 Mapper 接口
 *
 * <p>继承 BaseMapper<DevPole> 后，MyBatis-Plus 自动提供单表的 CRUD 方法：
 * insert / deleteById / updateById / selectById / selectList / selectPage / selectCount 等，
 * 无需手写 SQL 即可完成灯杆的增删改查。</p>
 *
 * <p>@Mapper 作用：让 Spring 容器扫描并生成代理实现类，可在 Service 里直接注入使用。
 * （项目若用了 @MapperScan 扫描包可省略 @Mapper，这里显式标注更直观，学习蓝本保留。）</p>
 *
 * <p>多表关联查询 BaseMapper 不支持，需自己写方法 + SQL（参考 SysUserMapper.selectUserRoleList）。
 * 当前灯杆的查询都是单表，暂不需要额外方法。</p>
 */
@Mapper
public interface DevPoleMapper extends BaseMapper<DevPole> {
}
