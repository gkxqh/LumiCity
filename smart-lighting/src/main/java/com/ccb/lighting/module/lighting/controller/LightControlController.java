package com.ccb.lighting.module.lighting.controller;

import com.ccb.lighting.common.Result;
import com.ccb.lighting.module.lighting.service.LightControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 照明实时控制 Controller
 *
 * <p>控制链路（LightControlService）：在线校验 → 模拟通信(sleep+95%成功率) → 写库 → 记日志
 * 离线/故障设备将被跳过，不会执行任何操作。</p>
 */
@RestController
@RequestMapping("/lighting/control")
@RequiredArgsConstructor
public class LightControlController {

    private final LightControlService lightControlService;

    @PostMapping("/switch")
    public Result<Map<String, Object>> switchLight(
            @RequestParam Long poleId,
            @RequestParam String action) {
        Map<String, Object> result = lightControlService.switchLight(poleId, action);
        String msg = (String) result.get("message");
        if ("灯杆不存在".equals(msg)) {
            return Result.error("灯杆不存在");
        }
        return Result.success(result);
    }

    @PostMapping("/brightness")
    public Result<Map<String, Object>> adjustBrightness(
            @RequestParam Long poleId,
            @RequestParam Integer brightness) {
        Map<String, Object> result = lightControlService.adjustBrightness(poleId, brightness);
        String msg = (String) result.get("message");
        if (msg != null && msg.startsWith("brightness")) {
            return Result.error("brightness 取值范围 0~100");
        }
        if ("灯杆不存在".equals(msg)) {
            return Result.error("灯杆不存在");
        }
        return Result.success(result);
    }

    @PostMapping("/batch/switch")
    public Result<Map<String, Object>> batchSwitchByRoad(
            @RequestParam String road,
            @RequestParam String action) {
        return Result.success(lightControlService.batchSwitchByRoad(road, action));
    }

    @PostMapping("/batch/brightness")
    public Result<Map<String, Object>> batchBrightnessByRoad(
            @RequestParam String road,
            @RequestParam Integer brightness) {
        Map<String, Object> result = lightControlService.batchBrightnessByRoad(road, brightness);
        String msg = (String) result.get("message");
        if (msg != null && msg.startsWith("brightness")) {
            return Result.error("brightness 取值范围 0~100");
        }
        return Result.success(result);
    }

    @PostMapping("/batch/switch-by-region")
    public Result<Map<String, Object>> batchSwitchByRegion(
            @RequestParam Long regionId,
            @RequestParam String action) {
        return Result.success(lightControlService.batchSwitchByRegion(regionId, action));
    }
}
