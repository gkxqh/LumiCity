package com.ccb.lighting.module.device.dto;

import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备查询 DTO（Data Transfer Object）
 *
 * <p>专门用于"设备分页查询"接口的参数接收，不直接用 DevDevice 实体，原因：
 * 1. 查询条件与实体字段并非一一对应（如 deviceName 需模糊查询，实体里没有"是否模糊"的标识）
 * 2. 实体上的 @NotBlank 校验会强制要求必填，但查询时所有条件都应可选
 * 3. 查询可能涉及跨表字段（如按区域查设备），实体里没有这些字段
 * 4. DTO 解耦了"接口入参"与"数据库实体"，二者可独立演进</p>
 *
 * <p>继承 PageQuery 自动拥有 current、size 分页字段，Controller 接收时：
 * GET /device/page?current=1&size=10&deviceName=路灯&deviceType=LIGHT&status=1&poleId=100</p>
 *
 * <p>注解说明：
 * - @Data：Lombok 生成 getter/setter
 * - @EqualsAndHashCode(callSuper = true)：继承 PageQuery，必须加此注解让父类字段参与 equals/hashCode</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceQueryDTO extends PageQuery {

    /** 设备名称：模糊查询条件（like '%xxx%'），为空时不拼此条件 */
    private String deviceName;

    /** 设备类型：精确查询（LIGHT/CAMERA/SENSOR/LED_SCREEN/BROADCAST），为空时不拼 */
    private String deviceType;

    /** 状态：精确查询（0离线/1在线/2故障），为空时不拼 */
    private Integer status;

    /** 所属灯杆ID：精确查询，用于查看某灯杆下挂载的所有设备，为空时不拼 */
    private Long poleId;
}
