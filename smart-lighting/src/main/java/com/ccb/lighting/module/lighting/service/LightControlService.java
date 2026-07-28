package com.ccb.lighting.module.lighting.service;

import com.ccb.lighting.module.device.entity.DevPole;

import java.util.List;
import java.util.Map;

/**
 * 照明实时控制 Service
 *
 * <p>核心职责：检查设备状态 → 模拟通信 → 写入状态 → 记录日志。
 * Controller 不再直接操作 Mapper，所有控制逻辑集中在此。</p>
 */
public interface LightControlService {

    /**
     * 单灯杆开关灯
     *
     * @param poleId 灯杆 ID
     * @param action on=开灯 off=关灯
     * @return 控制结果（含 poleName/lightStatus/message）
     */
    Map<String, Object> switchLight(Long poleId, String action);

    /**
     * 单灯杆调光
     *
     * @param poleId     灯杆 ID
     * @param brightness 亮度 0~100
     * @return 控制结果
     */
    Map<String, Object> adjustBrightness(Long poleId, Integer brightness);

    /**
     * 按道路批量开关灯
     *
     * @param road   道路名称
     * @param action on/off
     * @return 控制结果（含 affectedCount/skippedCount/failedCount）
     */
    Map<String, Object> batchSwitchByRoad(String road, String action);

    /**
     * 按道路批量调光
     *
     * @param road       道路名称
     * @param brightness 亮度 0~100
     * @return 控制结果
     */
    Map<String, Object> batchBrightnessByRoad(String road, Integer brightness);

    /**
     * 按区域批量开关灯
     *
     * @param regionId 区域 ID
     * @param action   on/off
     * @return 控制结果
     */
    Map<String, Object> batchSwitchByRegion(Long regionId, String action);
}
