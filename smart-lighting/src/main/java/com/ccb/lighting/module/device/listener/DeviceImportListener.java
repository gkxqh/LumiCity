package com.ccb.lighting.module.device.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.module.device.dto.DeviceImportDTO;
import com.ccb.lighting.module.device.entity.DevDevice;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.mapper.DevDeviceMapper;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * EasyExcel 设备导入监听器
 *
 * <p>继承 AnalysisEventListener，在读取 Excel 每行数据时触发 invoke 方法，
 * 读完所有数据后触发 doAfterAllAnalysed 方法。</p>
 *
 * <p>处理流程：
 * 1. 每行数据解析为 DeviceImportDTO
 * 2. 校验必填字段、数据格式
 * 3. 批量收集数据（避免频繁入库）
 * 4. 全部读完后一次性批量插入</p>
 */
@Slf4j
public class DeviceImportListener extends AnalysisEventListener<DeviceImportDTO> {

    /** 批量插入阈值，达到此数量时先批量插入 */
    private static final int BATCH_SIZE = 100;

    /** 设备 Mapper，用于入库和查重 */
    private final DevDeviceMapper devDeviceMapper;

    /** 灯杆 Mapper，用于校验灯杆是否存在 */
    private final DevPoleMapper devPoleMapper;

    /** 临时存储待插入的设备数据 */
    private final List<DevDevice> deviceList = new ArrayList<>();

    /** 已存在的设备编号集合，用于查重 */
    private final Set<String> existingCodes = new HashSet<>();

    /** 统计成功导入数量 */
    private int successCount = 0;

    /** 统计失败数量 */
    private int failCount = 0;

    /** 失败原因列表 */
    private final List<String> failReasons = new ArrayList<>();

    /**
     * 构造器注入 Mapper
     * 注意：Listener 不是 Spring Bean，需手动传入 Mapper 实例
     */
    public DeviceImportListener(DevDeviceMapper devDeviceMapper, DevPoleMapper devPoleMapper) {
        this.devDeviceMapper = devDeviceMapper;
        this.devPoleMapper = devPoleMapper;
        // 提前加载已存在的设备编号，避免每行都查数据库
        loadExistingCodes();
    }

    /**
     * 加载已存在的设备编号到内存集合
     */
    private void loadExistingCodes() {
        LambdaQueryWrapper<DevDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(DevDevice::getDeviceCode);
        List<DevDevice> existingDevices = devDeviceMapper.selectList(wrapper);
        for (DevDevice device : existingDevices) {
            existingCodes.add(device.getDeviceCode());
        }
    }

    /**
     * 每读取一行数据触发此方法
     *
     * @param dto     当前行解析出的数据
     * @param context 解析上下文
     */
    @Override
    public void invoke(DeviceImportDTO dto, AnalysisContext context) {
        int rowIndex = context.readRowHolder().getRowIndex() + 1; // Excel 行号（从1开始）
        
        try {
            // 校验数据
            validateRow(dto, rowIndex);
            
            // 转换为实体
            DevDevice device = convertToEntity(dto);
            
            // 添加到批量列表
            deviceList.add(device);
            successCount++;
            
            // 达到批量阈值时先插入
            if (deviceList.size() >= BATCH_SIZE) {
                batchInsert();
            }
        } catch (Exception e) {
            failCount++;
            failReasons.add(String.format("第%d行：%s", rowIndex, e.getMessage()));
            log.warn("导入失败 - 第{}行：{}", rowIndex, e.getMessage());
        }
    }

    /**
     * 校验单行数据
     */
    private void validateRow(DeviceImportDTO dto, int rowIndex) {
        // 校验必填字段
        if (dto.getDeviceCode() == null || dto.getDeviceCode().trim().isEmpty()) {
            throw new BusinessException("设备编号不能为空");
        }
        if (dto.getDeviceName() == null || dto.getDeviceName().trim().isEmpty()) {
            throw new BusinessException("设备名称不能为空");
        }
        
        // 校验设备编号是否重复
        String code = dto.getDeviceCode().trim();
        if (existingCodes.contains(code)) {
            throw new BusinessException("设备编号已存在：" + code);
        }
        
        // 校验灯杆是否存在（传了 poleId 才校验）
        if (dto.getPoleId() != null) {
            DevPole pole = devPoleMapper.selectById(dto.getPoleId());
            if (pole == null) {
                throw new BusinessException("所属灯杆不存在，ID：" + dto.getPoleId());
            }
        }
        
        // 校验设备类型（可选，限制为枚举值）
        if (dto.getDeviceType() != null && !isValidDeviceType(dto.getDeviceType())) {
            throw new BusinessException("设备类型无效，有效值：LIGHT/CAMERA/SENSOR/LED_SCREEN/BROADCAST");
        }
        
        // 校验状态（可选，限制为 0/1/2）
        if (dto.getStatus() != null && dto.getStatus() < 0 && dto.getStatus() > 2) {
            throw new BusinessException("状态无效，有效值：0离线/1在线/2故障");
        }
    }

    /**
     * 校验设备类型是否有效
     */
    private boolean isValidDeviceType(String type) {
        return "LIGHT".equals(type) || "CAMERA".equals(type) || 
               "SENSOR".equals(type) || "LED_SCREEN".equals(type) || 
               "BROADCAST".equals(type);
    }

    /**
     * 将 DTO 转换为实体
     */
    private DevDevice convertToEntity(DeviceImportDTO dto) {
        DevDevice device = new DevDevice();
        device.setDeviceCode(dto.getDeviceCode().trim());
        device.setDeviceName(dto.getDeviceName().trim());
        device.setDeviceType(dto.getDeviceType());
        device.setPoleId(dto.getPoleId());
        device.setModel(dto.getModel());
        device.setVendor(dto.getVendor());
        device.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        return device;
    }

    /**
     * 批量插入数据
     */
    private void batchInsert() {
        if (!deviceList.isEmpty()) {
            devDeviceMapper.insertBatch(deviceList);
            // 更新已存在编号集合
            for (DevDevice device : deviceList) {
                existingCodes.add(device.getDeviceCode());
            }
            deviceList.clear();
        }
    }

    /**
     * 所有数据读完后触发此方法
     * 处理剩余的批量数据
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 插入剩余数据
        batchInsert();
        log.info("设备导入完成 - 成功：{} 条，失败：{} 条", successCount, failCount);
    }

    /**
     * 获取导入结果统计
     */
    public ImportResult getResult() {
        return new ImportResult(successCount, failCount, failReasons);
    }

    /**
     * 导入结果封装
     */
    public static class ImportResult {
        private final int successCount;
        private final int failCount;
        private final List<String> failReasons;

        public ImportResult(int successCount, int failCount, List<String> failReasons) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.failReasons = failReasons;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public List<String> getFailReasons() {
            return failReasons;
        }
    }
}