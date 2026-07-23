package com.ccb.lighting.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.device.dto.PoleQueryDTO;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.ccb.lighting.module.device.service.DevPoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 灯杆 Service 实现类
 *
 * <p>注解说明：
 * - @Service：标记为业务层 Bean，交给 Spring 容器管理，Controller 才能注入
 * - @RequiredArgsConstructor：Lombok 生成带 final 字段的构造器，等价于
 *   public DevPoleServiceImpl(DevPoleMapper devPoleMapper){ this.devPoleMapper = devPoleMapper; }
 *   Spring 通过构造器把 devPoleMapper 注入进来，这就是"构造器注入"。</p>
 *
 * <p>为什么用构造器注入而不是 @Autowired 字段注入？
 * 1. 字段 final，注入后不可变，更安全
 * 2. 依赖关系一目了然，避免"循环依赖"隐患
 * 3. 不依赖 Spring 容器也能 db_tool.py 出对象做单元测试（传 mock）</p>
 *
 * <p>为什么注入 Mapper 而不是其他 Service？
 * 业务层操作数据库要经过 Mapper，所以注入 DevPoleMapper。
 * MyBatis-Plus 的 BaseMapper 已提供单表 CRUD，直接调用即可。</p>
 */
@Service
@RequiredArgsConstructor
public class DevPoleServiceImpl implements DevPoleService {

    /** 灯杆 Mapper，构造器注入（final 必须在构造器里赋值，@RequiredArgsConstructor 帮我们做了） */
    private final DevPoleMapper devPoleMapper;

    /**
     * 分页查询灯杆列表
     *
     * <p>LambdaQueryWrapper：MyBatis-Plus 的条件构造器，用 Lambda 方式写字段名，
     * 编译期检查字段名是否写错（比字符串 "pole_name" 安全）。
     * like 是模糊查询：pole_name like '%xxx%'；eq 是等值。</p>
     */
    @Override
    public IPage<DevPole> pageList(PoleQueryDTO query) {
        // 1. 构造查询条件：仅当传入值非空时才拼接，避免查全表
        LambdaQueryWrapper<DevPole> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            // 灯杆名称模糊查询：pole_name like '%xxx%'
            if (StringUtils.hasText(query.getPoleName())) {
                wrapper.like(DevPole::getPoleName, query.getPoleName());
            }
            // 灯杆编号精确查询（编号是唯一编码，无需模糊）
            if (StringUtils.hasText(query.getPoleCode())) {
                wrapper.eq(DevPole::getPoleCode, query.getPoleCode());
            }
            // 状态精确查询：0离线/1在线/2故障
            if (query.getStatus() != null) {
                wrapper.eq(DevPole::getStatus, query.getStatus());
            }
            // 区域ID精确查询：筛选某区域下所有灯杆
            if (query.getAreaId() != null) {
                wrapper.eq(DevPole::getAreaId, query.getAreaId());
            }
        }
        // 按创建时间倒序，最新灯杆排前面
        wrapper.orderByDesc(DevPole::getCreateTime);

        // 2. 执行分页查询：db_tool.py Page<>(current, size)，MyBatis-Plus 自动拼 limit
        //    返回类型是 Page（实现类），但方法声明返回 IPage（接口），子类赋父类合法
        return devPoleMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询灯杆
     * selectById 是 BaseMapper 自带方法，内部自动过滤逻辑删除的数据
     */
    @Override
    public DevPole getById(Long id) {
        return devPoleMapper.selectById(id);
    }

    /**
     * 新增灯杆
     * 关键步骤：编号查重 → 入库
     */
    @Override
    public void add(DevPole pole) {
        // 1. 灯杆编号查重：用 LambdaQueryWrapper 查是否已存在同编号灯杆
        //    poleCode 是业务唯一编码，重复会导致数据混乱
        Long count = devPoleMapper.selectCount(
                new LambdaQueryWrapper<DevPole>().eq(DevPole::getPoleCode, pole.getPoleCode())
        );
        if (count > 0) {
            // 抛业务异常，全局异常处理器会转成 Result 返回前端
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS);
        }

        // 2. 入库：insert 是 BaseMapper 自带方法
        //    createTime/updateTime/createBy 等由 MetaObjectHandler 自动填充，无需手动 set
        devPoleMapper.insert(pole);
    }

    /**
     * 修改灯杆
     * updateById 按 id 更新非 null 字段
     */
    @Override
    public void update(DevPole pole) {
        devPoleMapper.updateById(pole);
    }

    /**
     * 删除灯杆
     * 因 BaseEntity.deleted 上有 @TableLogic，deleteById 实际执行 update set deleted=1（逻辑删除）
     */
    @Override
    public void delete(Long id) {
        devPoleMapper.deleteById(id);
    }

    /**
     * 查询全部灯杆（不分页）
     * 用途：前端下拉框选择灯杆。数据量不大时直接全量返回，避免多次请求
     */
    @Override
    public List<DevPole> list() {
        // 按创建时间倒序，与分页查询保持一致的排序
        return devPoleMapper.selectList(
                new LambdaQueryWrapper<DevPole>().orderByDesc(DevPole::getCreateTime)
        );
    }
}
