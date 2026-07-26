package com.ccb.lighting.module.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 通用数据监听器
 *
 * <p>参照 EasyExcel 官方 DemoDataListener 设计，用于通用的 Excel 导入解析。
 * 其他模块可以继承此类并重写相关方法，或创建自己的专用监听器。</p>
 *
 * <p>使用示例：
 * <pre>{@code
 * ExcelDataListener listener = new ExcelDataListener();
 * EasyExcel.read(inputStream, ExcelData.class, listener).sheet().doRead();
 * List<ExcelData> data = listener.getData();
 * }</pre></p>
 *
 * @param <T> 数据类型
 */
@Slf4j
public class ExcelDataListener<T> extends AnalysisEventListener<T> {

    /** 解析到的数据列表 */
    private final List<T> dataList = new ArrayList<>();

    /** 导入结果 */
    private ImportResult result = new ImportResult();

    /**
     * 每解析一条数据就会调用一次
     *
     * @param data    解析后的数据对象
     * @param context 解析上下文
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        log.debug("解析到一条数据: {}", data);
        dataList.add(data);

        // 可以在这里添加数据校验逻辑
        if (!validateData(data)) {
            result.addError(context.readRowHolder().getRowIndex(), "数据校验失败");
        }
    }

    /**
     * 表头解析完成后调用
     *
     * @param headMap 表头数据（key为列索引，value为表头名称）
     * @param context 解析上下文
     */
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        log.info("表头解析完成: {}", headMap);
        result.setHeader(headMap);
    }

    /**
     * 所有数据解析完成后调用
     *
     * @param context 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("Excel 解析完成，共 {} 条数据", dataList.size());
        result.setTotalCount(dataList.size());
        result.setSuccessCount(dataList.size() - result.getErrorCount());
    }

    /**
     * 数据转换异常时调用
     *
     * @param exception 异常信息
     * @param context   解析上下文
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("Excel 解析异常", exception);

        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException convertException = (ExcelDataConvertException) exception;
            String errorMsg = String.format("第 %d 行，第 %d 列数据转换异常: %s",
                    convertException.getRowIndex(),
                    convertException.getColumnIndex(),
                    exception.getMessage());
            result.addError(convertException.getRowIndex(), errorMsg);
        } else {
            result.addError(context.readRowHolder().getRowIndex(), exception.getMessage());
        }
    }

    /**
     * 数据校验（子类可重写）
     *
     * @param data 待校验的数据
     * @return true 校验通过，false 校验失败
     */
    protected boolean validateData(T data) {
        // 默认校验：数据不为空
        return data != null;
    }

    /**
     * 获取解析后的数据列表
     *
     * @return 数据列表
     */
    public List<T> getData() {
        return dataList;
    }

    /**
     * 获取导入结果
     *
     * @return 导入结果
     */
    public ImportResult getResult() {
        return result;
    }

    /**
     * 导入结果类
     */
    @lombok.Data
    public static class ImportResult {

        /** 表头数据 */
        private Map<Integer, String> header;

        /** 总行数 */
        private int totalCount;

        /** 成功行数 */
        private int successCount;

        /** 错误行数 */
        private int errorCount;

        /** 错误详情列表 */
        private List<ErrorDetail> errors = new ArrayList<>();

        /**
         * 添加错误信息
         *
         * @param rowIndex 行号（Excel中的行号）
         * @param message  错误信息
         */
        public void addError(int rowIndex, String message) {
            errors.add(new ErrorDetail(rowIndex, message));
            errorCount++;
        }

        /**
         * 错误详情
         */
        @lombok.Data
        @lombok.AllArgsConstructor
        public static class ErrorDetail {
            /** 行号 */
            private int rowIndex;
            /** 错误信息 */
            private String message;
        }
    }
}
