package com.ccb.lighting.module.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.device.dto.DeviceQueryDTO;
import com.ccb.lighting.module.device.entity.DevDevice;
import com.ccb.lighting.module.device.listener.DeviceImportListener;
import com.ccb.lighting.module.device.service.DevDeviceService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备 Controller
 *
 * <p>路径前缀 /device，提供设备管理的增删改查（CRUD）接口。
 * 这些接口都需要登录后才能访问（由 JwtInterceptor 拦截）。</p>
 *
 * <p>RESTful 风格约定：
 * - GET    /device/page    分页查询（查询用 GET，参数走 URL）
 * - GET    /device/{id}    查详情
 * - POST   /device         新增（请求体带数据）
 * - PUT    /device         修改
 * - DELETE /device/{id}    删除
 * - POST   /device/import  批量导入（上传 Excel）
 * - GET    /device/export  批量导出（下载 Excel）</p>
 *
 * <p>Controller 层职责单一：接收参数 → 调 Service → 包装 Result 返回。
 * 不写业务逻辑，业务逻辑全在 Service 实现类里。</p>
 */
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DevDeviceController {

    /** 设备 Service，构造器注入 */
    private final DevDeviceService devDeviceService;

    /**
     * 分页查询设备列表
     *
     * <p>请求示例：GET /device/page?current=1&size=10&deviceName=路灯&deviceType=LIGHT&status=1&poleId=100
     * Spring 自动把参数绑定到 DeviceQueryDTO 对象（字段名对应），交给 Service 处理。</p>
     *
     * @param query 查询条件（含分页参数 current/size，及业务筛选条件）
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<DevDevice>> page(DeviceQueryDTO query) {
        IPage<DevDevice> page = devDeviceService.pageList(query);
        return Result.success(page);
    }

    /**
     * 根据 id 查询设备详情
     *
     * @param id 设备 ID，@PathVariable 从 URL 路径取值
     * @return 设备信息
     */
    @GetMapping("/{id}")
    public Result<DevDevice> getById(@PathVariable Long id) {
        DevDevice device = devDeviceService.getById(id);
        return Result.success(device);
    }

    /**
     * 新增设备
     *
     * <p>请求体示例：{"deviceCode":"L-2024-0001","deviceName":"人民路1号路灯","deviceType":"LIGHT","poleId":100,"model":"LED-100W","vendor":"欧普","status":1}
     * Service 层会查重 deviceCode、校验 poleId 是否存在后再入库。</p>
     *
     * @param device 设备信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DevDevice device) {
        devDeviceService.add(device);
        return Result.success();
    }

    /**
     * 修改设备
     *
     * <p>请求体需带 id。@Valid 校验必填字段（如 deviceCode/deviceName 不能为空）。</p>
     *
     * @param device 设备信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody DevDevice device) {
        devDeviceService.update(device);
        return Result.success();
    }

    /**
     * 根据 id 删除设备（逻辑删除）
     *
     * @param id 设备 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        devDeviceService.delete(id);
        return Result.success();
    }

    /**
     * 批量导入设备
     *
     * <p>前端通过表单上传 Excel 文件，后端使用 EasyExcel 解析并批量入库。
     * Excel 模板格式需包含：设备编号、设备名称、设备类型、所属灯杆ID、设备型号、厂商、状态。</p>
     *
     * @param file 上传的 Excel 文件
     * @return 导入结果（成功/失败数量及失败原因）
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importDevices(@RequestParam("file") MultipartFile file) throws IOException {
        // 校验文件
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error("仅支持 Excel 文件（.xlsx 或 .xls）");
        }

        // 调用 Service 导入
        DeviceImportListener.ImportResult result = devDeviceService.importDevices(file.getInputStream());

        // 封装结果
        Map<String, Object> data = new HashMap<>();
        data.put("successCount", result.getSuccessCount());
        data.put("failCount", result.getFailCount());
        data.put("failReasons", result.getFailReasons());

        return Result.success("导入完成", data);
    }

    /**
     * 查询所有设备（不分页）
     * 返回所有设备的 deviceCode + deviceName，用于模拟告警等场景前端随机选取设备
     */
    @GetMapping("/listAll")
    public Result<List<DevDevice>> listAll() {
        return Result.success(devDeviceService.listAll());
    }

    /**
     * 批量导出设备
     *
     * <p>查询所有设备数据，使用 EasyExcel 写入 Excel 文件并返回给前端下载。</p>
     *
     * @param response HTTP 响应对象，用于设置下载头信息
     */
    @GetMapping("/export")
    public void exportDevices(HttpServletResponse response) throws IOException {
        // 设置响应头
        String filename = "设备列表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        // 调用 Service 导出
        devDeviceService.exportDevices(response.getOutputStream());
    }
}