package com.ccb.lighting.module.excel.service;

import com.ccb.lighting.module.excel.listener.ExcelDataListener;
import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Excel 通用服务接口
 *
 * <p>三层架构中的"业务层"接口，提供 Excel 导入导出的通用能力。
 * Controller 只依赖此接口，不依赖实现类。</p>
 *
 * <p>方法说明：
 * - export：通用导出，将数据列表导出为 Excel 文件
 * - importExcel：通用导入，解析 Excel 文件为数据列表</p>
 */
public interface ExcelService {

    /**
     * 导出Excel文件
     * @param response 响应对象
     * @param filename 文件名
     * @param data 数据
     * @param clazz 数据类型
     * @return true:导出成功 false:导出失败
     */
    boolean export(HttpServletResponse response,
                   String filename,
                   Collection<?> data,
                   Class<?> clazz);

}