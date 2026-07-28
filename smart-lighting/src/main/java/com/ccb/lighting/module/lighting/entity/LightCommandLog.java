package com.ccb.lighting.module.lighting.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 照明控制指令日志
 *
 * <p>记录每次照明控制的完整链路信息，包含指令类型、值、模拟通信结果等。
 * 作为操作审计与故障排查的依据。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("light_command_log")
public class LightCommandLog extends BaseEntity implements Serializable {

    /** 灯杆 ID */
    private Long poleId;

    /** 灯杆名称（冗余，避免跨表 join） */
    private String poleName;

    /** 指令类型：SWITCH 开关 / BRIGHTNESS 调光 */
    private String commandType;

    /** 指令值：on/off（开关时）或 0~100 数字字符串（调光时） */
    private String commandValue;

    /** 模拟通信状态：SUCCESS 成功 / FAIL 失败 / SKIPPED 跳过(离线) */
    private String simStatus;

    /** 模拟通信结果描述 */
    private String simMessage;
}
