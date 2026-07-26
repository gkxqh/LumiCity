package com.ccb.lighting.module.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccb.lighting.common.BusinessException;
import com.ccb.lighting.common.ResultCode;
import com.ccb.lighting.module.video.dto.VideoCameraQueryDTO;
import com.ccb.lighting.module.video.entity.VideoCamera;
import com.ccb.lighting.module.video.mapper.VideoCameraMapper;
import com.ccb.lighting.module.video.service.VideoCameraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 视频摄像头 Service 实现类
 *
 * <p>简单 CRUD，按创建时间倒序排列。</p>
 */
@Service
@RequiredArgsConstructor
public class VideoCameraServiceImpl implements VideoCameraService {

    /** 摄像头 Mapper，构造器注入 */
    private final VideoCameraMapper videoCameraMapper;

    /**
     * 根据 id 查询摄像头
     */
    @Override
    public VideoCamera getById(Long id) {
        VideoCamera camera = videoCameraMapper.selectById(id);
        if (camera == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return camera;
    }

    @Override
    public IPage<VideoCamera> pageListByQuery(VideoCameraQueryDTO query) {
        LambdaQueryWrapper<VideoCamera> wrapper = new LambdaQueryWrapper<>();
        if(query.getStatus()!=null){
            wrapper.eq(VideoCamera::getStatus,query.getStatus());
        }
        if(query.getCameraName()!=null){
            wrapper.like(VideoCamera::getCameraName,query.getCameraName());
        }
        wrapper.orderByDesc(VideoCamera::getCreateTime);
        return videoCameraMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                wrapper
        );

    }

    /**
     * 新增摄像头
     */
    @Override
    public void add(VideoCamera camera) {
        // 默认状态为离线，待设备上报心跳后变在线
        if (camera.getStatus() == null) {
            camera.setStatus(0);
        }
        // 默认不支持云台
        if (camera.getPtzEnable() == null) {
            camera.setPtzEnable(0);
        }
        videoCameraMapper.insert(camera);
    }

    /**
     * 修改摄像头
     */
    @Override
    public void update(VideoCamera camera) {
        if (videoCameraMapper.selectById(camera.getId()) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        videoCameraMapper.updateById(camera);
    }

    /**
     * 删除摄像头
     */
    @Override
    public void delete(Long id) {
        videoCameraMapper.deleteById(id);
    }
}
