package com.ccb.lighting.module.lighting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.ccb.lighting.module.lighting.entity.LightCommandLog;
import com.ccb.lighting.module.lighting.mapper.LightCommandLogMapper;
import com.ccb.lighting.module.lighting.service.LightControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 照明控制 Service 实现
 *
 * <p>控制链路：
 * 1. 查询灯杆信息，校验是否存在
 * 2. 检查在线状态：离线设备直接跳过，不执行任何操作
 * 3. 模拟通信：sleep(300~800ms)，95%成功率模拟真实设备通信
 * 4. 写入数据库：更新 light_status / light_brightness
 * 5. 记录指令日志：入库 light_command_log 作为审计追溯</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LightControlServiceImpl implements LightControlService {

    private final DevPoleMapper devPoleMapper;
    private final LightCommandLogMapper commandLogMapper;

    /**
     * 检查灯杆是否在线并可控制
     *
     * @param pole 灯杆
     * @return 不在线时返回错误信息，在线返回 null
     */
    private String checkOffline(DevPole pole) {
        if (pole.getStatus() != null && pole.getStatus() != 1) {
            String reason = pole.getStatus() == 0 ? "灯杆离线(状态=0)" : "灯杆故障(状态=" + pole.getStatus() + ")";
            log.warn("控制被拦截：{} poleId={} poleName={}", reason, pole.getId(), pole.getPoleName());
            return reason;
        }
        return null;
    }

    /**
     * 模拟与设备的 MQTT/WebSocket 通信
     *
     * @return true=通信成功 false=通信失败
     */
    private boolean simulateCommunication() {
        // 模拟网络环境：95% 成功率
        return Math.random() < 0.95;
    }

    /**
     * 执行单个灯杆的控制逻辑：校验 → 通信 → 写库 → 记日志
     *
     * @param pole        灯杆
     * @param commandType 指令类型：SWITCH/BRIGHTNESS
     * @param commandVal  指令值：on/off 或亮度数字
     * @return 控制结果描述
     */
    private Map<String, Object> executeSingle(DevPole pole, String commandType, String commandVal,
                                               Runnable dbUpdate) {
        Map<String, Object> result = new HashMap<>();
        result.put("poleId", pole.getId());

        // 1. 在线校验
        String offlineMsg = checkOffline(pole);
        if (offlineMsg != null) {
            log.warn("跳过离线灯杆控制：{} poleId={}", offlineMsg, pole.getId());
            // 记录跳过日志
            saveCommandLog(pole, commandType, commandVal, "SKIPPED", offlineMsg);
            result.put("simStatus", "SKIPPED");
            result.put("message", offlineMsg + "，已跳过");
            return result;
        }

        // 2. 模拟通信
        boolean commOk = simulateCommunication();
        if (!commOk) {
            log.warn("通信失败：poleId={}", pole.getId());
            saveCommandLog(pole, commandType, commandVal, "FAIL", "模拟通信失败，设备未响应");
            result.put("simStatus", "FAIL");
            result.put("message", "通信失败，设备未响应");
            return result;
        }

        // 3. 写入数据库
        dbUpdate.run();
        devPoleMapper.updateById(pole);

        // 4. 记录日志
        saveCommandLog(pole, commandType, commandVal, "SUCCESS", "通信成功，指令已执行");

        result.put("simStatus", "SUCCESS");
        result.put("poleName", pole.getPoleName());
        result.put("lightStatus", pole.getLightStatus());
        result.put("lightBrightness", pole.getLightBrightness());
        // 组装成功消息
        if ("SWITCH".equals(commandType)) {
            boolean on = "on".equals(commandVal);
            result.put("message", on ? "开灯成功" : "关灯成功");
        } else {
            result.put("message", "调光成功，亮度=" + commandVal);
        }
        return result;
    }

    /**
     * 记录指令日志
     */
    private void saveCommandLog(DevPole pole, String commandType, String commandValue,
                                 String simStatus, String simMessage) {
        LightCommandLog logEntry = new LightCommandLog();
        logEntry.setPoleId(pole.getId());
        logEntry.setPoleName(pole.getPoleName());
        logEntry.setCommandType(commandType);
        logEntry.setCommandValue(commandValue);
        logEntry.setSimStatus(simStatus);
        logEntry.setSimMessage(simMessage);
        commandLogMapper.insert(logEntry);
    }

    // ========================================================================
    // 接口实现
    // ========================================================================

    @Override
    public Map<String, Object> switchLight(Long poleId, String action) {
        DevPole pole = devPoleMapper.selectById(poleId);
        if (pole == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "灯杆不存在");
            return err;
        }

        boolean on = "on".equals(action);
        return executeSingle(pole, "SWITCH", action, () -> {
            pole.setLightStatus(on ? 1 : 0);
            if (on) {
                // 开灯时恢复默认亮度 80%；若之前已有亮度则保留
                if (pole.getLightBrightness() == null || pole.getLightBrightness() == 0) {
                    pole.setLightBrightness(80);
                }
            } else {
                pole.setLightBrightness(0);
            }
        });
    }

    @Override
    public Map<String, Object> adjustBrightness(Long poleId, Integer brightness) {
        if (brightness < 0 || brightness > 100) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "brightness 取值范围 0~100");
            return err;
        }

        DevPole pole = devPoleMapper.selectById(poleId);
        if (pole == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "灯杆不存在");
            return err;
        }

        return executeSingle(pole, "BRIGHTNESS", String.valueOf(brightness), () -> {
            pole.setLightBrightness(brightness);
            pole.setLightStatus(brightness > 0 ? 1 : 0);
        });
    }

    @Override
    public Map<String, Object> batchSwitchByRoad(String road, String action) {
        List<DevPole> poles = devPoleMapper.selectList(
                new LambdaQueryWrapper<DevPole>()
                        .eq(DevPole::getRoad, road)
                        .eq(DevPole::getDeleted, 0)
        );

        boolean on = "on".equals(action);
        int success = 0, skipped = 0, failed = 0;

        for (DevPole pole : poles) {
            Map<String, Object> single = executeSingle(pole, "SWITCH", action, () -> {
                pole.setLightStatus(on ? 1 : 0);
                if (on) {
                    if (pole.getLightBrightness() == null || pole.getLightBrightness() == 0) {
                        pole.setLightBrightness(80);
                    }
                } else {
                    pole.setLightBrightness(0);
                }
            });
            String status = (String) single.get("simStatus");
            switch (status == null ? "" : status) {
                case "SUCCESS" -> success++;
                case "SKIPPED" -> skipped++;
                case "FAIL" -> failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("road", road);
        result.put("action", action);
        result.put("total", poles.size());
        result.put("successCount", success);
        result.put("skippedCount", skipped);
        result.put("failedCount", failed);
        result.put("message", String.format("批量操作完成：共 %d 个灯杆（成功 %d，跳过 %d(离线)，失败 %d）",
                poles.size(), success, skipped, failed));
        return result;
    }

    @Override
    public Map<String, Object> batchBrightnessByRoad(String road, Integer brightness) {
        if (brightness < 0 || brightness > 100) {
            Map<String, Object> err = new HashMap<>();
            err.put("message", "brightness 取值范围 0~100");
            return err;
        }

        List<DevPole> poles = devPoleMapper.selectList(
                new LambdaQueryWrapper<DevPole>()
                        .eq(DevPole::getRoad, road)
                        .eq(DevPole::getDeleted, 0)
        );

        int success = 0, skipped = 0, failed = 0;

        for (DevPole pole : poles) {
            Map<String, Object> single = executeSingle(pole, "BRIGHTNESS", String.valueOf(brightness), () -> {
                pole.setLightBrightness(brightness);
                pole.setLightStatus(brightness > 0 ? 1 : 0);
            });
            String status = (String) single.get("simStatus");
            switch (status == null ? "" : status) {
                case "SUCCESS" -> success++;
                case "SKIPPED" -> skipped++;
                case "FAIL" -> failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("road", road);
        result.put("brightness", brightness);
        result.put("total", poles.size());
        result.put("successCount", success);
        result.put("skippedCount", skipped);
        result.put("failedCount", failed);
        result.put("message", String.format("批量调光完成：共 %d 个灯杆（成功 %d，跳过 %d(离线)，失败 %d）",
                poles.size(), success, skipped, failed));
        return result;
    }

    @Override
    public Map<String, Object> batchSwitchByRegion(Long regionId, String action) {
        List<DevPole> poles = devPoleMapper.selectList(
                new LambdaQueryWrapper<DevPole>()
                        .eq(DevPole::getRegionId, regionId)
                        .eq(DevPole::getDeleted, 0)
        );

        boolean on = "on".equals(action);
        int success = 0, skipped = 0, failed = 0;

        for (DevPole pole : poles) {
            Map<String, Object> single = executeSingle(pole, "SWITCH", action, () -> {
                pole.setLightStatus(on ? 1 : 0);
                if (on) {
                    if (pole.getLightBrightness() == null || pole.getLightBrightness() == 0) {
                        pole.setLightBrightness(80);
                    }
                } else {
                    pole.setLightBrightness(0);
                }
            });
            String status = (String) single.get("simStatus");
            switch (status == null ? "" : status) {
                case "SUCCESS" -> success++;
                case "SKIPPED" -> skipped++;
                case "FAIL" -> failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("regionId", regionId);
        result.put("action", action);
        result.put("total", poles.size());
        result.put("successCount", success);
        result.put("skippedCount", skipped);
        result.put("failedCount", failed);
        result.put("message", String.format("批量操作完成：共 %d 个灯杆（成功 %d，跳过 %d(离线)，失败 %d）",
                poles.size(), success, skipped, failed));
        return result;
    }
}
