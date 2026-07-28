package com.ccb.lighting.module.device.service;

import com.ccb.lighting.module.device.entity.Region;

import java.util.List;

/**
 * 区域 Service 接口
 */
public interface RegionService {

    /**
     * 查询所有区域（不分页）
     */
    List<Region> list();

    /**
     * 根据 id 查询区域
     */
    Region getById(Long id);

    /**
     * 新增区域（名称查重）
     */
    void add(Region region);

    /**
     * 修改区域
     */
    void update(Region region);

    /**
     * 删除区域（有灯杆关联时拒绝）
     */
    void delete(Long id);
}
