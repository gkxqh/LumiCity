package com.ccb.lighting.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.device.entity.DevDevice;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 设备 Mapper 接口
 *
 * <p>继承 BaseMapper<DevDevice> 后，MyBatis-Plus 自动提供单表的 CRUD 方法：
 * insert / deleteById / updateById / selectById / selectList / selectPage / selectCount 等，
 * 无需手写 SQL 即可完成设备的增删改查。</p>
 *
 * <p>@Mapper 作用：让 Spring 容器扫描并生成代理实现类，可在 Service 里直接注入使用。
 * （项目若用了 @MapperScan 扫描包可省略 @Mapper，这里显式标注更直观，学习蓝本保留。）</p>
 *
 * <p>多表关联查询 BaseMapper 不支持，需自己写方法 + SQL。
 * 例如若需"查设备时连带返回灯杆名称"，可在此写 join 查询方法。当前需求单表即可满足。</p>
 */
@Mapper
public interface DevDeviceMapper extends BaseMapper<DevDevice> {

    /**
     * 批量插入设备数据
     *
     * @param list 设备列表
     */
    void insertBatch(List<DevDevice> list);
}
