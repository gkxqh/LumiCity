package com.ccb.lighting.module.excel.controller;

import com.ccb.lighting.module.excel.vo.ExcelVO;
import com.ccb.lighting.module.excel.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Excel 通用控制器
 *
 * <p>提供通用的 Excel 导入导出接口，其他模块可以调用此控制器完成 Excel 操作。</p>
 *
 * <p>接口列表：
 * - GET  /excel/export          通用导出（示例）
 * - POST /excel/import          通用导入
 * </p>
 */
@RestController
@RequestMapping("/excel")
public class ExcelController {
    @Autowired
    private  ExcelService excelService;

    /**
     * 通用导出示例
     *
     * <p>演示如何使用 ExcelService 导出数据。
     * 其他模块可以参考此方法，创建自己的导出接口。</p>
     *
     * <p>请求示例：GET /excel/export?fileName=测试数据&sheetName=数据列表</p>
     *
     * @param response   HTTP响应对象
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        //构造数据
        List<ExcelVO> data = new ArrayList<>();
        ExcelVO item1 = new ExcelVO();
        item1.setString("示例数据1");
        item1.setDateTime(LocalDateTime.now().minusDays(2));
        item1.setDoubleData(123.45);
        data.add(item1);

        excelService.export(response, "测试数据", data, ExcelVO.class);


    }

    /**
     * 通用导入接口
     *
     * <p>上传 Excel 文件并解析为数据列表。
     * 其他模块可以参考此方法，创建自己的导入接口。</p>
     *
     * <p>请求示例：POST /excel/import（multipart/form-data，file 参数为 Excel 文件）</p>
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果
     */
    /*@PostMapping("/import")
    public Result<ExcelDataListener.ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件为空");
        }

        try {
            // 调用通用导入方法
            ExcelDataListener.ImportResult result = excelService.importExcel(
                    file.getInputStream(),
                    ExcelVO.class
            );

            if (result.getErrorCount() > 0) {
                throw new BusinessException("导入完成，但有 " + result.getErrorCount() + " 条数据存在错误");
            }

            return Result.success(result);
        } catch (IOException e) {
            return Result.error("文件读取失败：" + e.getMessage());
        }
    }*/

    /**
     * 创建示例数据（演示用）
     *
     * @return 示例数据列表
     */
   /* private List<ExcelData> createDemoData() {
        List<ExcelData> data = new ArrayList<>();

        ExcelData item1 = new ExcelData();
        item1.setString("示例数据1");
        item1.setDateTime(LocalDateTime.now().minusDays(2));
        item1.setDoubleData(123.45);
        data.add(item1);

        ExcelData item2 = new ExcelData();
        item2.setString("示例数据2");
        item2.setDateTime(LocalDateTime.now().minusDays(1));
        item2.setDoubleData(678.90);
        data.add(item2);

        ExcelData item3 = new ExcelData();
        item3.setString("示例数据3");
        item3.setDateTime(LocalDateTime.now());
        item3.setDoubleData(234.56);
        data.add(item3);

        return data;
    }*/
}