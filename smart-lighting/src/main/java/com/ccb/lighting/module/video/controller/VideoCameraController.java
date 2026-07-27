package com.ccb.lighting.module.video.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.video.dto.VideoCameraQueryDTO;
import com.ccb.lighting.module.video.entity.VideoCamera;
import com.ccb.lighting.module.video.service.VideoCameraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频摄像头 Controller
 *
 * <p>路径前缀 /video/camera，提供摄像头的 CRUD + 获取播放地址 + 抓拍图接口。
 * 前端通过播放地址接入视频播放器（如原生 video / img 直连）实现在线监控；
 * 对浏览器无法直连的 RTSP 流，前端回退调用 /snapshot 由后端实时绘制抓拍图兜底。</p>
 *
 * <p>接口列表：
 * - GET    /video/camera/page              分页查询
 * - GET    /video/camera/{id}              查详情
 * - POST   /video/camera                    新增
 * - PUT    /video/camera                    修改
 * - DELETE /video/camera/{id}               删除
 * - GET    /video/camera/{id}/stream       获取播放地址
 * - GET    /video/camera/{id}/snapshot     抓拍图（RTSP 等无法直连时的演示兜底）</p>
 */
@RestController
@RequestMapping("/video/camera")
@RequiredArgsConstructor
public class VideoCameraController {

    /** 视频摄像头 Service，构造器注入 */
    private final VideoCameraService videoCameraService;

    /**
     * 分页查询摄像头列表
     *
     * @param query 分页参数
     * @return 分页数据
     */
    @GetMapping("/page")
    public Result<IPage<VideoCamera>> page(VideoCameraQueryDTO query) {
        return Result.success(videoCameraService.pageListByQuery(query));
    }

    /**
     * 根据 id 查询摄像头详情
     *
     * @param id 摄像头 ID
     * @return 摄像头信息
     */
    @GetMapping("/{id}")
    public Result<VideoCamera> getById(@PathVariable Long id) {
        return Result.success(videoCameraService.getById(id));
    }

    /**
     * 新增摄像头
     *
     * @param camera 摄像头信息
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody VideoCamera camera) {
        videoCameraService.add(camera);
        return Result.success();
    }

    /**
     * 修改摄像头
     *
     * @param camera 摄像头信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody VideoCamera camera) {
        videoCameraService.update(camera);
        return Result.success();
    }

    /**
     * 根据 id 删除摄像头
     *
     * @param id 摄像头 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        videoCameraService.delete(id);
        return Result.success();
    }

    /**
     * 获取播放地址
     *
     * <p>请求示例：GET /video/camera/100/stream
     * 返回该摄像头的 RTSP 流地址，前端播放器据此拉流。</p>
     *
     * <p>真实场景可能还要返回：
     * - HLS/FLV 转码地址（如 https://xxx/live/100.m3u8）
     * - 播放凭证（防盗链 token）
     * - 备用流地址（主备双流切换）</p>
     *
     * @param id 摄像头 ID
     * @return 包含 streamUrl 的 Map
     */
    @GetMapping("/{id}/stream")
    public Result<Map<String, Object>> getStreamUrl(@PathVariable Long id) {
        VideoCamera camera = videoCameraService.getById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("cameraId", camera.getId());
        result.put("cameraName", camera.getCameraName());
        result.put("streamUrl", camera.getStreamUrl());
        result.put("resolution", camera.getResolution());
        return Result.success(result);
    }

    /**
     * 获取摄像头抓拍图（演示 / 兜底）
     *
     * <p>浏览器无法直连 RTSP 时，由后端用 Java AWT 实时绘制一帧监控风格 JPEG 返回，
     * 前端以轮询方式模拟实时画面。真实场景应替换为流媒体网关截帧或设备抓拍。</p>
     *
     * @param id 摄像头 ID
     * @return JPEG 图片字节
     */
    @GetMapping(value = "/{id}/snapshot", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> snapshot(@PathVariable Long id) {
        byte[] data = videoCameraService.getSnapshot(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
    }
}
