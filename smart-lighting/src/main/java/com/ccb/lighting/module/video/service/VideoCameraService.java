package com.ccb.lighting.module.video.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.video.dto.VideoCameraQueryDTO;
import com.ccb.lighting.module.video.entity.VideoCamera;

/**
 * 视频摄像头 Service 接口
 *
 * <p>方法清单：
 * - getById：根据 id 查摄像头详情
 * - add：新增摄像头
 * - update：修改摄像头
 * - delete：删除摄像头</p>
 */
public interface VideoCameraService {

    /**
     * 根据 id 查询摄像头详情
     *
     * @param id 摄像头 ID
     * @return 摄像头实体
     */
    VideoCamera getById(Long id);

    IPage<VideoCamera> pageListByQuery(VideoCameraQueryDTO query);

    /**
     * 新增摄像头
     *
     * @param camera 摄像头信息
     */
    void add(VideoCamera camera);

    /**
     * 修改摄像头
     *
     * @param camera 摄像头信息（含 id）
     */
    void update(VideoCamera camera);

    /**
     * 根据 id 删除摄像头
     *
     * @param id 摄像头 ID
     */
    void delete(Long id);
}
