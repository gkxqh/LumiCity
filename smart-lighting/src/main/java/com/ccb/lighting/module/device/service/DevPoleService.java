package com.ccb.lighting.module.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.device.dto.PoleQueryDTO;
import com.ccb.lighting.module.device.entity.DevPole;

import java.util.List;

/**
 * 灯杆 Service 接口
 *
 * <p>三层架构中的"业务层"接口。Controller 只依赖此接口，不依赖实现类，
 * 面向接口编程，便于切换实现、做单元测试 mock。</p>
 *
 * <p>方法说明：
 * - pageList：分页 + 条件查询灯杆列表（支持按名称模糊、编号、状态、区域筛选）
 * - getById：根据 id 查单个灯杆（详情页用）
 * - add：新增灯杆（含编号查重）
 * - update：修改灯杆
 * - delete：根据 id 删除灯杆（逻辑删除）
 * - list：查全部灯杆（不分页，给下拉框用）</p>
 *
 * <p>返回值用 IPage（接口）而非 Page（实现类）：面向接口编程，
 * Service 实现可自由切换分页实现（如 Page / 前端分页插件），不影响调用方。</p>
 */
public interface DevPoleService {

    /**
     * 分页查询灯杆列表
     *
     * @param query 查询条件（含分页参数 current/size，及业务筛选条件）
     * @return 分页对象，含 records（数据）、total（总数）、current、size 等
     */
    IPage<DevPole> pageList(PoleQueryDTO query);

    /**
     * 根据 id 查询灯杆详情
     *
     * @param id 灯杆 ID
     * @return 灯杆实体，不存在返回 null
     */
    DevPole getById(Long id);

    /**
     * 新增灯杆
     * 内部逻辑：校验灯杆编号是否重复 → 入库
     *
     * @param pole 灯杆信息（编号、名称等）
     */
    void add(DevPole pole);

    /**
     * 修改灯杆
     *
     * @param pole 待更新的灯杆信息（需含 id）
     */
    void update(DevPole pole);

    /**
     * 根据 id 删除灯杆（BaseEntity 的 @TableLogic 让 MyBatis-Plus 自动改为逻辑删除）
     *
     * @param id 灯杆 ID
     */
    void delete(Long id);

    /**
     * 查询全部灯杆（不分页）
     * 用途：前端下拉框选择灯杆时调用，数据量不大时直接全量返回
     *
     * @return 灯杆列表
     */
    List<DevPole> list();
}
