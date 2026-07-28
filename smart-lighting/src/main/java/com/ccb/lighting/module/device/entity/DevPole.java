package com.ccb.lighting.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ccb.lighting.common.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 灯杆实体 DevPole
 *
 * <p>智慧城市照明系统中，灯杆是物理载体，所有设备（照明灯、摄像头、传感器、LED屏、广播）
 * 都挂载在灯杆上。本表管理灯杆本身的基础信息与地理位置。</p>
 *
 * <p>灯杆的地址由三个独立字段组成：region_id（所属区）、road（路/街/大道）、number（号），
 * 前端展示时的 pole_name 和 address 由服务层自动拼接（{区}{路}{号}灯杆），前端不可编辑。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dev_pole")
public class DevPole extends BaseEntity implements Serializable {

    /** 灯杆编号：业务唯一编码（如 "P-2024-001"），新增时需查重 */
    @NotBlank(message = "灯杆编号不能为空")
    private String poleCode;

    /** 灯杆名称：由服务层自动拼接（{区}{路}{号}灯杆），前端只读展示不自编辑 */
    private String poleName;

    /** 所属区域ID：关联 region 表，可为空（未分区时） */
    private Long regionId;

    /** 区域名称：非数据库字段，查询时通过 region_id 关联填充（前端展示用） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String regionName;

    /** 道路/街/大道名称：如"科华北路""天府大道"，按此字段做批量控制和分组 */
    private String road;

    /** 编号：如"88号""29号院"，与 regionId + road 拼接成完整地址 */
    private String number;

    /** 安装地址：由服务层自动拼接（{区}{路}{号}），前端只读展示不自编辑 */
    private String address;

    /** 经度：地图坐标，与 lat 配合在 GIS 地图上标点 */
    private java.math.BigDecimal lng;

    /** 纬度：地图坐标 */
    private java.math.BigDecimal lat;

    /** 灯杆高度（米） */
    private java.math.BigDecimal height;

    /** 在线状态：0=离线，1=在线，2=故障（设备连通性，非照明开关） */
    private Integer status;

    /** 照明状态：0=关灯，1=开灯（控制接口操作此字段） */
    private Integer lightStatus;

    /** 当前亮度：0~100（控制接口操作此字段） */
    private Integer lightBrightness;

    /** 安装时间 */
    private LocalDate installTime;
}
