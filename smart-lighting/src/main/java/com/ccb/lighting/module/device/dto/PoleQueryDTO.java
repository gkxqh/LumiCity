package com.ccb.lighting.module.device.dto;

import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 灯杆查询 DTO
 *
 * <p>支持按 poleName/poleCode/status/regionId/road 筛选灯杆。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PoleQueryDTO extends PageQuery {

    /** 灯杆名称：模糊查询 */
    private String poleName;

    /** 灯杆编号：精确查询 */
    private String poleCode;

    /** 状态：精确查询（0离线/1在线/2故障） */
    private Integer status;

    /** 区域ID：精确查询 */
    private Long regionId;

    /** 道路名称：精确查询（按路批量控制时使用） */
    private String road;
}
