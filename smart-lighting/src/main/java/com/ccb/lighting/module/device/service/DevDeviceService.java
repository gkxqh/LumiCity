package com.ccb.lighting.module.device.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.device.dto.DeviceQueryDTO;
import com.ccb.lighting.module.device.entity.DevDevice;

/**
 * 设备 Service 接口
 *
 * <p>三层架构中的"业务层"接口。Controller 只依赖此接口，不依赖实现类，
 * 面向接口编程，便于切换实现、做单元测试 mock。</p>
 *
 * <p>方法说明：
 * - pageList：分页 + 条件查询设备列表（支持按名称模糊、类型、状态、灯杆筛选）
 * - getById：根据 id 查单个设备（详情页用）
 * - add：新增设备（含编号查重、灯杆存在性校验）
 * - update：修改设备
 * - delete：根据 id 删除设备（逻辑删除）</p>
 *
 * <p>返回值用 IPage（接口）而非 Page（实现类）：面向接口编程，
 * Service 实现可自由切换分页实现，不影响调用方。</p>
 */
public interface DevDeviceService {

    /**
     * 分页查询设备列表
     *
     * @param query 查询条件（含分页参数 current/size，及业务筛选条件）
     * @return 分页对象，含 records（数据）、total（总数）、current、size 等
     */
    IPage<DevDevice> pageList(DeviceQueryDTO query);

    /**
     * 根据 id 查询设备详情
     *
     * @param id 设备 ID
     * @return 设备实体，不存在返回 null
     */
    DevDevice getById(Long id);

    /**
     * 新增设备
     * 内部逻辑：校验设备编号是否重复 → 校验所属灯杆是否存在 → 入库
     *
     * @param device 设备信息
     */
    void add(DevDevice device);

    /**
     * 修改设备
     *
     * @param device 待更新的设备信息（需含 id）
     */
    void update(DevDevice device);

    /**
     * 根据 id 删除设备（BaseEntity 的 @TableLogic 让 MyBatis-Plus 自动改为逻辑删除）
     *
     * @param id 设备 ID
     */
    void delete(Long id);

    /**
     * 导入设备数据
     *
     * @param inputStream Excel 文件输入流
     * @return 导入结果（成功/失败数量及原因）
     */
    DeviceImportListener.ImportResult importDevices(java.io.InputStream inputStream);

    /**
     * 导出设备数据
     *
     * @param outputStream 输出流
     */
    void exportDevices(java.io.OutputStream outputStream);
}
