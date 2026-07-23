package com.ccb.lighting.module.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.device.dto.DeviceQueryDTO;
import com.ccb.lighting.module.device.entity.DevDevice;
import com.ccb.lighting.module.device.service.DevDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
 * - DELETE /device/{id}    删除</p>
 *
 * <p>Controller 层职责单一：接收参数 → 调 Service → 包装 Result 返回。
 * 不写业务逻辑，业务逻辑全在 Service 实现类里。</p>
 *
 * <p>参数校验：POST/PUT 接口加 @Valid，触发 DevDevice 实体上的 @NotBlank 校验，
 * 校验失败由全局异常处理器转成 Result 返回前端。</p>
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
     * <p>用 DTO 接收而非散装参数：参数多时 DTO 更清晰，且 DTO 继承 PageQuery 自带分页字段。</p>
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
     * <p>@Valid：触发 DevDevice 实体上的 @NotBlank 校验，deviceCode/deviceName 为空时直接返回 400。</p>
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
}
