package com.ccb.lighting.module.device.dto;

import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 灯杆查询 DTO
 *
 * <p>专门用于"灯杆分页查询"接口的参数接收。与 DeviceQueryDTO 同理，
 * 查询条件独立成 DTO，避免实体上的 @NotBlank 校验干扰查询参数接收。</p>
 *
 * <p>继承 PageQuery 自动拥有 current、size 分页字段，Controller 接收时：
 * GET /device/pole/page?current=1&size=10&poleName=人民路&status=1&areaId=10</p>
 *
 * <p>注解说明：
 * - @Data：Lombok 生成 getter/setter
 * - @EqualsAndHashCode(callSuper = true)：继承 PageQuery，必须加此注解让父类字段参与 equals/hashCode</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PoleQueryDTO extends PageQuery {

    /** 灯杆名称：模糊查询条件（like '%xxx%'），为空时不拼此条件 */
    private String poleName;

    /** 灯杆编号：精确查询，为空时不拼。按编号查特定灯杆时用 */
    private String poleCode;

    /** 状态：精确查询（0离线/1在线/2故障），为空时不拼。运维大屏常按"故障"筛选 */
    private Integer status;

    /** 区域ID：精确查询，为空时不拼。按区域统计、筛选灯杆时用 */
    private Long areaId;
}
