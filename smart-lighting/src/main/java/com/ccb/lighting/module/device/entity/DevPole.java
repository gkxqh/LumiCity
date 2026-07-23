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
 * <p>继承 BaseEntity 自动拥有 id、createTime、updateTime、createBy、updateBy、deleted 字段，
 * 这里只声明灯杆特有的业务字段。</p>
 *
 * <p>注解说明：
 * - @TableName("dev_pole")：指定数据库表名，显式写出更清晰
 * - @EqualsAndHashCode(callSuper = true)：继承场景下必须加，让 equals/hashCode 把父类字段也算进去
 * - @NotBlank：Jakarta Bean Validation 注解，配合 Controller 的 @Valid 做参数校验，
 *   表示字符串不能为 null、空串、空白串。校验失败由全局异常处理器转成 Result 返回前端。</p>
 *
 * <p>表结构参考（学习时建表用）：
 * CREATE TABLE dev_pole (
 *   id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *   pole_code VARCHAR(50) NOT NULL COMMENT '灯杆编号（唯一）',
 *   pole_name VARCHAR(100) NOT NULL COMMENT '灯杆名称',
 *   area_id BIGINT COMMENT '所属区域ID',
 *   address VARCHAR(255) COMMENT '安装地址',
 *   lng DECIMAL(10,7) COMMENT '经度',
 *   lat DECIMAL(10,7) COMMENT '纬度',
 *   height DECIMAL(5,2) COMMENT '灯杆高度(米)',
 *   status TINYINT DEFAULT 0 COMMENT '状态：0离线 1在线 2故障',
 *   install_time DATE COMMENT '安装时间',
 *   create_time DATETIME, update_time DATETIME,
 *   create_by BIGINT, update_by BIGINT, deleted TINYINT DEFAULT 0
 * );</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dev_pole")
public class DevPole extends BaseEntity implements Serializable {

    /** 灯杆编号：业务唯一编码（如 "P-2024-001"），新增时需查重，前端必填 */
    @NotBlank(message = "灯杆编号不能为空")
    private String poleCode;

    /** 灯杆名称：用于前端展示（如 "人民路1号灯杆"），前端必填 */
    @NotBlank(message = "灯杆名称不能为空")
    private String poleName;

    /** 所属区域ID：关联区域表，可为空（未分区时）；用于按区域统计、筛选灯杆 */
    private Long areaId;

    /** 安装地址：文字描述的安装位置，便于运维人员现场定位 */
    private String address;

    /** 经度：地图坐标，与 lat 配合在 GIS 地图上标点。用 BigDecimal 存储避免浮点精度丢失 */
    private java.math.BigDecimal lng;

    /** 纬度：地图坐标，与 lng 配合在 GIS 地图上标点 */
    private java.math.BigDecimal lat;

    /** 灯杆高度（米）：物理属性，用于计算照明覆盖范围 */
    private java.math.BigDecimal height;

    /** 状态：0=离线，1=在线，2=故障。由设备上报或定时巡检更新，前端按状态显示不同颜色 */
    private Integer status;

    /** 安装时间：记录灯杆投入使用的时间，用于计算使用年限、保修期 */
    private LocalDate installTime;
}
