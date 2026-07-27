package com.ccb.lighting.module.device.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 设备导入 DTO（EasyExcel 读取用）
 *
 * <p>@ExcelProperty 的 value 必须与 Excel 模板的标题行完全一致，
 * EasyExcel 会按标题匹配列，顺序不影响解析。</p>
 *
 * <p>Excel 模板格式：
 * | 设备编号 | 设备名称 | 设备类型 | 所属灯杆ID | 设备型号 | 厂商 | 状态 |
 * |---------|---------|---------|-----------|---------|------|------|
 * | L-001   | 路灯1号  | LIGHT   | 1         | LED-50W | 欧普 | 1    |</p>
 */
@Data
public class DeviceImportDTO {

    @ExcelProperty("设备编号")
    private String deviceCode;

    @ExcelProperty("设备名称")
    private String deviceName;

    @ExcelProperty("设备类型")
    private String deviceType;

    @ExcelProperty("所属灯杆ID")
    private Long poleId;

    @ExcelProperty("设备型号")
    private String model;

    @ExcelProperty("厂商")
    private String vendor;

    @ExcelProperty("状态")
    private Integer status;
}

