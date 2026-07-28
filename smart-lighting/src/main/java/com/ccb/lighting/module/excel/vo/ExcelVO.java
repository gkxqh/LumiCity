package com.ccb.lighting.module.excel.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Excel 通用数据类
 *
 * <p>参照 EasyExcel 官方 DemoData 设计，用于通用的 Excel 导出和导入。
 * 其他模块可以继承或组合此类，或创建自己的专用数据类。</p>
 * 建议自己写，因为这个类过于简单了，仅作参考。
 * <p>使用示例：
 * <pre>{@code
 * // 导出时直接使用
 * List<ExcelData> data = new ArrayList<>();
 * ExcelData item = new ExcelData();
 * item.setString("设备名称");
 * item.setDateTime(LocalDateTime.now());
 * item.setDoubleData(123.45);
 * data.add(item);
 *
 * // 使用 EasyExcel 写出
 * EasyExcel.write(outputStream, ExcelData.class).sheet("Sheet1").doWrite(data);
 * }</pre></p>
 */
@Data
public class ExcelVO {

    /** 字符串字段：用于通用文本数据 */
    @ExcelProperty("文本字段")
    private String string;

    /** 日期时间字段：用于时间数据 */
    @ExcelProperty("日期时间")
    private LocalDateTime dateTime;

    /** 数值字段：用于数字数据 */
    @ExcelProperty("数值字段")
    private Double doubleData;
}