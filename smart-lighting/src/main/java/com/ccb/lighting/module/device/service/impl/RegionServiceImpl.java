package com.ccb.lighting.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.entity.Region;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.ccb.lighting.module.device.mapper.RegionMapper;
import com.ccb.lighting.module.device.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 区域 Service 实现
 */
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;
    private final DevPoleMapper devPoleMapper;

    @Override
    public List<Region> list() {
        return regionMapper.selectList(
                new LambdaQueryWrapper<Region>()
                        .orderByAsc(Region::getSort)
                        .orderByAsc(Region::getId)
        );
    }

    @Override
    public Region getById(Long id) {
        return regionMapper.selectById(id);
    }

    @Override
    public void add(Region region) {
        Long count = regionMapper.selectCount(
                new LambdaQueryWrapper<Region>().eq(Region::getName, region.getName())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS);
        }
        regionMapper.insert(region);
    }

    @Override
    public void update(Region region) {
        // 名称变更时查重
        Region old = regionMapper.selectById(region.getId());
        if (old != null && !old.getName().equals(region.getName())) {
            Long count = regionMapper.selectCount(
                    new LambdaQueryWrapper<Region>()
                            .eq(Region::getName, region.getName())
                            .ne(Region::getId, region.getId())
            );
            if (count > 0) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS);
            }
        }
        regionMapper.updateById(region);
    }

    @Override
    public void delete(Long id) {
        // 检查是否有灯杆关联此区域
        Long poleCount = devPoleMapper.selectCount(
                new LambdaQueryWrapper<DevPole>().eq(DevPole::getRegionId, id)
        );
        if (poleCount > 0) {
            throw new BusinessException(ResultCode.DATA_IN_USE);
        }
        regionMapper.deleteById(id);
    }
}
