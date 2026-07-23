package com.ccb.lighting.module.lighting.controller;

import com.ccb.lighting.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 照明实时控制 Controller
 *
 * <p>路径前缀 /lighting/control，提供对单个灯杆的实时开关与调光接口。
 * 与策略（Strategy）的区别：策略是定时/规则触发的"自动"控制；
 * 控制接口是运维人员手动操作，立即下发到设备。</p>
 *
 * <p>当前骨架为简化实现，直接返回成功响应。
 * 真实场景应通过 MQTT/WebSocket 下发指令到灯杆控制器，并等待设备 ACK 后再返回。</p>
 *
 * <p>接口列表：
 * - POST /lighting/control/switch      开关灯（参数 poleId + action=on/off）
 * - POST /lighting/control/brightness  调光（参数 poleId + brightness=0~100）</p>
 */
@RestController
@RequestMapping("/lighting/control")
@RequiredArgsConstructor
public class LightControlController {

    /**
     * 开关灯接口
     *
     * <p>请求示例：POST /lighting/control/switch?poleId=100&action=on
     * action 取值：on=开灯，off=关灯</p>
     *
     * <p>真实场景：通过 MQTT 主题 cmd/pole/{poleId} 下发 {"action":"on"} 指令，
     * 设备执行后回报状态，更新 dev_device.status 与灯杆在线状态。</p>
     *
     * @param poleId 灯杆 ID
     * @param action 操作：on 开灯 / off 关灯
     * @return 操作结果（包含下发的指令信息）
     */
    @PostMapping("/switch")
    public Result<Map<String, Object>> switchLight(
            @RequestParam Long poleId,
            @RequestParam String action) {
        // TODO: 真实场景通过 MQTT/WebSocket 下发指令到设备
        Map<String, Object> result = new HashMap<>();
        result.put("poleId", poleId);
        result.put("action", action);
        result.put("message", "指令已下发，等待设备回报");
        return Result.success(result);
    }

    /**
     * 调光接口
     *
     * <p>请求示例：POST /lighting/control/brightness?poleId=100&brightness=80
     * brightness 取值 0~100，0 等同关灯，100 为最大亮度。</p>
     *
     * @param poleId     灯杆 ID
     * @param brightness 亮度值（0~100）
     * @return 操作结果
     */
    @PostMapping("/brightness")
    public Result<Map<String, Object>> adjustBrightness(
            @RequestParam Long poleId,
            @RequestParam Integer brightness) {
        // TODO: 真实场景下发调光指令到灯杆控制器
        if (brightness < 0 || brightness > 100) {
            return Result.error("brightness 取值范围 0~100");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("poleId", poleId);
        result.put("brightness", brightness);
        result.put("message", "调光指令已下发");
        return Result.success(result);
    }
}
