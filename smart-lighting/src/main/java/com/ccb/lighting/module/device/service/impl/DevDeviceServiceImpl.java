package com.ccb.lighting.module.device.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.device.dto.DeviceImportDTO;
import com.ccb.lighting.module.device.dto.DeviceQueryDTO;
import com.ccb.lighting.module.device.entity.DevDevice;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.listener.DeviceImportListener;
import com.ccb.lighting.module.device.mapper.DevDeviceMapper;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.ccb.lighting.module.device.service.DevDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 设备 Service 实现类
 *
 * <p>注解说明：
 * - @Service：标记为业务层 Bean，交给 Spring 容器管理，Controller 才能注入
 * - @RequiredArgsConstructor：Lombok 生成带 final 字段的构造器，Spring 通过构造器注入依赖</p>
 *
 * <p>为什么同时注入 DevDeviceMapper 和 DevPoleMapper？
 * - DevDeviceMapper：设备的 CRUD 操作
 * - DevPoleMapper：新增设备时校验所属灯杆是否存在（跨表校验，需查灯杆表）
 * 这是"业务层组合多个 Mapper"的典型场景，一个 Service 可依赖多个 Mapper。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevDeviceServiceImpl implements DevDeviceService {

    /** 设备 Mapper，构造器注入，负责设备表的 CRUD */
    private final DevDeviceMapper devDeviceMapper;

    /** 灯杆 Mapper，构造器注入，用于新增设备时校验灯杆是否存在 */
    private final DevPoleMapper devPoleMapper;

    /**
     * 分页查询设备列表
     *
     * <p>LambdaQueryWrapper：MyBatis-Plus 的条件构造器，用 Lambda 方式写字段名，
     * 编译期检查字段名是否写错（比字符串 "device_name" 安全）。</p>
     */
    @Override
    public IPage<DevDevice> pageList(DeviceQueryDTO query) {
        // 1. 构造查询条件：仅当传入值非空时才拼接，避免查全表
        LambdaQueryWrapper<DevDevice> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            // 设备名称模糊查询：device_name like '%xxx%'
            if (StringUtils.hasText(query.getDeviceName())) {
                wrapper.like(DevDevice::getDeviceName, query.getDeviceName());
            }
            // 设备类型精确查询：LIGHT/CAMERA/SENSOR/LED_SCREEN/BROADCAST
            if (StringUtils.hasText(query.getDeviceType())) {
                wrapper.eq(DevDevice::getDeviceType, query.getDeviceType());
            }
            // 状态精确查询：0离线/1在线/2故障
            if (query.getStatus() != null) {
                wrapper.eq(DevDevice::getStatus, query.getStatus());
            }
            // 所属灯杆ID精确查询：查某灯杆下挂载的所有设备
            if (query.getPoleId() != null) {
                wrapper.eq(DevDevice::getPoleId, query.getPoleId());
            }
        }
        // 按创建时间倒序，最新设备排前面
        wrapper.orderByDesc(DevDevice::getCreateTime);

        // 2. 执行分页查询：db_tool.py Page<>(current, size)，MyBatis-Plus 自动拼 limit
        return devDeviceMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );
    }

    /**
     * 根据 id 查询设备
     * selectById 是 BaseMapper 自带方法，内部自动过滤逻辑删除的数据
     */
    @Override
    public DevDevice getById(Long id) {
        return devDeviceMapper.selectById(id);
    }

    /**
     * 新增设备
     * 关键步骤：设备编号查重 → 校验所属灯杆是否存在 → 入库
     */
    @Override
    public void add(DevDevice device) {
        // 1. 设备编号查重：deviceCode 是业务唯一编码，重复会导致数据混乱
        Long count = devDeviceMapper.selectCount(
                new LambdaQueryWrapper<DevDevice>().eq(DevDevice::getDeviceCode, device.getDeviceCode())
        );
        if (count > 0) {
            // 抛业务异常，全局异常处理器会转成 Result 返回前端
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS);
        }

        // 2. 校验所属灯杆是否存在（可选校验，传了 poleId 才校验）
        //    防止设备挂载到不存在的灯杆上，造成数据孤儿
        if (device.getPoleId() != null) {
            DevPole pole = devPoleMapper.selectById(device.getPoleId());
            if (pole == null) {
                throw new BusinessException("所属灯杆不存在，请检查 poleId");
            }
        }

        // 3. 入库：insert 是 BaseMapper 自带方法
        //    createTime/updateTime/createBy 等由 MetaObjectHandler 自动填充，无需手动 set
        devDeviceMapper.insert(device);
    }

    /**
     * 修改设备
     * updateById 按 id 更新非 null 字段
     */
    @Override
    public void update(DevDevice device) {
        devDeviceMapper.updateById(device);
    }

    /**
     * 删除设备
     * 因 BaseEntity.deleted 上有 @TableLogic，deleteById 实际执行 update set deleted=1（逻辑删除）
     */
    @Override
    public void delete(Long id) {
        devDeviceMapper.deleteById(id);
    }

    /**
     * 导入设备数据
     * 使用 EasyExcel 读取上传的 Excel 文件，通过自定义监听器处理每行数据
     */
    @Override
    public DeviceImportListener.ImportResult importDevices(InputStream inputStream) {
        DeviceImportListener listener = new DeviceImportListener(devDeviceMapper, devPoleMapper);
        try {
            EasyExcel.read(inputStream, DeviceImportDTO.class, listener)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            log.error("设备导入失败", e);
            throw new BusinessException("导入失败：" + e.getMessage());
        }
        return listener.getResult();
    }

    /**
     * 查询所有设备（不分页）
     * 用于模拟告警等场景前端需要随机选取设备
     */
    @Override
    public List<DevDevice> listAll() {
        return devDeviceMapper.selectList(
                new LambdaQueryWrapper<DevDevice>()
                        .select(DevDevice::getDeviceCode, DevDevice::getDeviceName)
                        .orderByDesc(DevDevice::getCreateTime)
        );
    }

    /**
     * 导出设备数据
     * 使用 EasyExcel 写入所有设备数据到输出流
     * 直接使用 DevDevice 实体（已添加 @ExcelProperty 注解），无需转换
     */
    @Override
    public void exportDevices(OutputStream outputStream) {
        // 查询所有设备数据
        LambdaQueryWrapper<DevDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DevDevice::getCreateTime);
        List<DevDevice> devices = devDeviceMapper.selectList(wrapper);

        try {
            // 直接使用 DevDevice 实体，@ExcelProperty 注解定义了 Excel 列标题
            EasyExcel.write(outputStream, DevDevice.class)
                    .sheet("设备列表")
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .doWrite(devices);
        } catch (Exception e) {
            log.error("设备导出失败", e);
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }
}
