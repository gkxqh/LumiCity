package com.ccb.lighting.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.device.dto.PoleQueryDTO;
import com.ccb.lighting.module.device.entity.DevPole;
import com.ccb.lighting.module.device.entity.Region;
import com.ccb.lighting.module.device.mapper.DevPoleMapper;
import com.ccb.lighting.module.device.mapper.RegionMapper;
import com.ccb.lighting.module.device.service.DevPoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 灯杆 Service 实现类
 *
 * <p>新增/修改时自动根据 regionId + road + number 拼接 pole_name 和 address。
 * pole_name = "{区}{路}{号}灯杆"
 * address  = "{区}{路}{号}"
 * 区名从 region 表查询获取；regionId 为空时跳过区名拼接。</p>
 */
@Service
@RequiredArgsConstructor
public class DevPoleServiceImpl implements DevPoleService {

    private final DevPoleMapper devPoleMapper;
    private final RegionMapper regionMapper;

    /**
     * 根据 regionId 补充 regionName（前端表格展示用）
     */
    private void fillRegionName(DevPole pole) {
        if (pole.getRegionId() != null) {
            Region region = regionMapper.selectById(pole.getRegionId());
            if (region != null) {
                pole.setRegionName(region.getName());
            }
        }
    }

    /**
     * 根据 regionId + road + number 拼接 poleName 和 address
     */
    private void fillNameAndAddress(DevPole pole) {
        // 获取区名
        String regionName = "";
        if (pole.getRegionId() != null) {
            Region region = regionMapper.selectById(pole.getRegionId());
            if (region != null) {
                regionName = region.getName();
            }
        }

        String road = pole.getRoad() != null ? pole.getRoad() : "";
        String number = pole.getNumber() != null ? pole.getNumber() : "";

        // address = {区}{路}{号}，每段非空时拼接
        StringBuilder addrBuilder = new StringBuilder();
        if (!regionName.isEmpty()) addrBuilder.append(regionName);
        if (!road.isEmpty()) addrBuilder.append(road);
        if (!number.isEmpty()) addrBuilder.append(number);
        String address = addrBuilder.toString();

        // poleName = address + "灯杆"
        String poleName = address.isEmpty() ? "" : address + "灯杆";

        pole.setPoleName(poleName);
        pole.setAddress(address);
    }

    @Override
    public IPage<DevPole> pageList(PoleQueryDTO query) {
        LambdaQueryWrapper<DevPole> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText(query.getPoleName())) {
                wrapper.like(DevPole::getPoleName, query.getPoleName());
            }
            if (StringUtils.hasText(query.getPoleCode())) {
                wrapper.eq(DevPole::getPoleCode, query.getPoleCode());
            }
            if (query.getStatus() != null) {
                wrapper.eq(DevPole::getStatus, query.getStatus());
            }
            if (query.getRegionId() != null) {
                wrapper.eq(DevPole::getRegionId, query.getRegionId());
            }
            if (StringUtils.hasText(query.getRoad())) {
                wrapper.eq(DevPole::getRoad, query.getRoad());
            }
        }
        wrapper.orderByDesc(DevPole::getCreateTime);

        IPage<DevPole> page = devPoleMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );

        // 补充 regionName
        for (DevPole pole : page.getRecords()) {
            fillRegionName(pole);
        }

        return page;
    }

    @Override
    public DevPole getById(Long id) {
        DevPole pole = devPoleMapper.selectById(id);
        if (pole != null) {
            fillRegionName(pole);
        }
        return pole;
    }

    @Override
    public void add(DevPole pole) {
        // 编号查重
        Long count = devPoleMapper.selectCount(
                new LambdaQueryWrapper<DevPole>().eq(DevPole::getPoleCode, pole.getPoleCode())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS);
        }

        // 自动填充 pole_name 和 address
        fillNameAndAddress(pole);

        devPoleMapper.insert(pole);
    }

    @Override
    public void update(DevPole pole) {
        // 自动重新拼接 pole_name 和 address（region/road/number 任一变化都能反映）
        fillNameAndAddress(pole);

        devPoleMapper.updateById(pole);
    }

    @Override
    public void delete(Long id) {
        devPoleMapper.deleteById(id);
    }

    @Override
    public List<DevPole> list() {
        List<DevPole> list = devPoleMapper.selectList(
                new LambdaQueryWrapper<DevPole>().orderByDesc(DevPole::getCreateTime)
        );
        for (DevPole pole : list) {
            fillRegionName(pole);
        }
        return list;
    }
}
