package com.ccb.lighting.module.excel.service.impl;

import cn.idev.excel.FastExcel;
import com.alibaba.excel.EasyExcel;
import com.ccb.lighting.module.excel.listener.ExcelDataListener;
import com.ccb.lighting.module.excel.service.ExcelService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Excel 通用服务实现类
 *
 * <p>注解说明：
 * - @Service：标记为业务层 Bean，交给 Spring 容器管理，Controller 才能注入
 * - @RequiredArgsConstructor：Lombok 生成带 final 字段的构造器</p>
 *
 * <p>依赖说明：
 * - EasyExcel：阿里巴巴开源的 Excel 处理框架，支持大数据量流式读写，避免内存溢出</p>
 */
@Service
@Slf4j
public class ExcelServiceImpl implements ExcelService {


    @Override
    public boolean export(HttpServletResponse response, String filename, Collection<?> data, Class<?> clz) {
        try {
            if(data == null || data.isEmpty()){
                return false;
            }
            if(clz == null){
                //通过反射获取元素类型
                clz = data.iterator().next().getClass();
            }
            //设置mime:https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Guides/MIME_types/Common_types
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            //设置文件名编码
            String fileName = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            //设置响应头，指定文件名编码为utf-8，否则中文会显示乱码
            response.setHeader("Content-disposition",
                    "attachment;filename*=utf-8''" + fileName + ".xlsx");
            // 写入数据
            ServletOutputStream outputStream = response.getOutputStream();
            FastExcel.write(outputStream, clz)
                    .sheet("sheet1")//默认sheet1
                    .doWrite(data);
            //刷新输出流
            outputStream.flush();
            //关闭输出流
            outputStream.close();
            //导出成功
        } catch (IOException e) {
            // throw new RuntimeException(e);
            log.error("导出Excel文件失败，文件名：{}", filename, e);
            //导出失败
            return false;
        }
        return true;
    }
}