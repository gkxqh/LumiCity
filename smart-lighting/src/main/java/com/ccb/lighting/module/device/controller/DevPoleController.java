package com.ccb.lighting.module.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.device.dto.PoleQueryDTO;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.service.DevPoleService;
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

import java.util.List;

/**
 * 灯杆 Controller
 *
 * <p>路径前缀 /device/pole，提供灯杆管理的增删改查（CRUD）接口。
 * 这些接口都需要登录后才能访问（由 JwtInterceptor 拦截）。</p>
 *
 * <p>RESTful 风格约定：
 * - GET    /device/pole/page    分页查询（查询用 GET，参数走 URL）
 * - GET    /device/pole/list    查全部（不分页，给下拉框用）
 * - GET    /device/pole/{id}    查详情
 * - POST   /device/pole         新增（请求体带数据）
 * - PUT    /device/pole         修改
 * - DELETE /device/pole/{id}    删除</p>
 *
 * <p>Controller 层职责单一：接收参数 → 调 Service → 包装 Result 返回。
 * 不写业务逻辑，业务逻辑全在 Service 实现类里。</p>
 *
 * <p>参数校验：POST/PUT 接口加 @Valid，触发 DevPole 实体上的 @NotBlank 校验，
 * 校验失败由全局异常处理器转成 Result 返回前端。</p>
 */
@RestController
@RequestMapping("/device/pole")
@RequiredArgsConstructor
public class DevPoleController {

    /** 灯杆 Service，构造器注入 */
    private final DevPoleService devPoleService;

    /**
     * 分页查询灯杆列表
     *
     * <p>请求示例：GET /device/pole/page?current=1&size=10&poleName=人民路&status=1&areaId=10
     * Spring 自动把参数绑定到 PoleQueryDTO 对象（字段名对应），交给 Service 处理。</p>
     *
     * <p>用 DTO 接收而非散装参数：参数多时 DTO 更清晰，且 DTO 继承 PageQuery 自带分页字段。</p>
     *
     * @param query 查询条件（含分页参数 current/size，及业务筛选条件）
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<DevPole>> page(PoleQueryDTO query) {
        IPage<DevPole> page = devPoleService.pageList(query);
        return Result.success(page);
    }

    /**
     * 查询全部灯杆（不分页）
     *
     * <p>用途：前端下拉框选择灯杆时调用。数据量不大时直接全量返回，避免分页加载。</p>
     *
     * @return 灯杆列表
     */
    @GetMapping("/list")
    public Result<List<DevPole>> list() {
        List<DevPole> list = devPoleService.list();
        return Result.success(list);
    }

    /**
     * 根据 id 查询灯杆详情
     *
     * @param id 灯杆 ID，@PathVariable 从 URL 路径取值
     * @return 灯杆信息
     */
    @GetMapping("/{id}")
    public Result<DevPole> getById(@PathVariable Long id) {
        DevPole pole = devPoleService.getById(id);
        return Result.success(pole);
    }

    /**
     * 新增灯杆
     *
     * <p>请求体示例：{"poleCode":"P-2024-001","poleName":"人民路1号灯杆","areaId":10,"address":"人民路100号","lng":116.404,"lat":39.915,"height":8,"status":1,"installTime":"2024-01-15"}
     * Service 层会查重 poleCode 后再入库。</p>
     *
     * <p>@Valid：触发 DevPole 实体上的 @NotBlank 校验，poleCode/poleName 为空时直接返回 400。</p>
     *
     * @param pole 灯杆信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody DevPole pole) {
        devPoleService.add(pole);
        return Result.success();
    }

    /**
     * 修改灯杆
     *
     * <p>请求体需带 id。@Valid 校验必填字段（如 poleCode/poleName 不能为空）。</p>
     *
     * @param pole 灯杆信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody DevPole pole) {
        devPoleService.update(pole);
        return Result.success();
    }

    /**
     * 根据 id 删除灯杆（逻辑删除）
     *
     * @param id 灯杆 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        devPoleService.delete(id);
        return Result.success();
    }
}
