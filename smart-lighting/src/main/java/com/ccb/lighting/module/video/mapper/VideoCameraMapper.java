package com.ccb.lighting.module.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccb.lighting.module.video.entity.VideoCamera;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频摄像头 Mapper 接口
 *
 * <p>继承 BaseMapper<VideoCamera> 自动拥有单表 CRUD 方法。</p>
 */
@Mapper
public interface VideoCameraMapper extends BaseMapper<VideoCamera> {
}
