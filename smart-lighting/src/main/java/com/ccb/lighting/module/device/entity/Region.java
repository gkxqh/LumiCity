package com.ccb.lighting.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 行政区划实体 Region
 *
 * <p>扁平化的区级列表（取代原来的树形 area 表）。每个区域包含若干条道路，
 * 灯杆通过 region_id 关联到此表。不再支持树形层级，"城市→区→街道→路段"
 * 中的街道/路段逻辑由 dev_pole.road 字段覆盖。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("region")
public class Region extends BaseEntity implements Serializable {

    /** 区域名称：如"武侯区""锦江区" */
    @NotBlank(message = "区域名称不能为空")
    private String name;

    /** 排序值 */
    private Integer sort;

    /** 状态：0禁用 1启用 */
    private Integer status;
}
