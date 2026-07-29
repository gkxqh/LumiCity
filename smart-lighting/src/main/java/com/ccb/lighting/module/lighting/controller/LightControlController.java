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
 * <p>控制链路（LightControlService）：在线校验 → 模拟通信 → 写库 → 记日志
 * 离线/故障设备将不会收到指令。</p>
 */
@RestController
@RequestMapping("/lighting/control")
@RequiredArgsConstructor
public class LightControlController {

    private final LightControlService lightControlService;

    @PostMapping("/switch")//单杆控制，制定灯杆ID+开关
    public Result<Map<String, Object>> switchLight(
            @RequestParam Long poleId,//灯杆ID
            @RequestParam String action) {//开关
        Map<String, Object> result = lightControlService.switchLight(poleId, action);
        //调LightControlService的switchLight方法，执行单灯开关，把返回结果存入result这个Map
        String msg = (String) result.get("message");
        if ("灯杆不存在".equals(msg)) {
            return Result.error("灯杆不存在");
        }
        return Result.success(result);
    }

    @PostMapping("/brightness")//单杆控制，制定灯杆ID+亮度
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

    @PostMapping("/batch/switch")//批量控制，指定道路名称
    public Result<Map<String, Object>> batchSwitchByRoad(
            @RequestParam String road,
            @RequestParam String action) {
        return Result.success(lightControlService.batchSwitchByRoad(road, action));
    }

    @PostMapping("/batch/brightness")//按照道路批量调节亮度
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

    @PostMapping("/batch/switch-by-region")//按区域批量开关
    public Result<Map<String, Object>> batchSwitchByRegion(
            @RequestParam Long regionId,
            @RequestParam String action) {
        return Result.success(lightControlService.batchSwitchByRegion(regionId, action));
    }
}
