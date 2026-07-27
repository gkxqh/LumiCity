package com.ccb.lighting.module.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ccb.lighting.module.video.dto.VideoCameraQueryDTO;
import com.ccb.lighting.module.video.entity.VideoCamera;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 视频摄像头 Mapper 接口
 *
 * <p>继承 BaseMapper<VideoCamera> 自动拥有单表 CRUD 方法。</p>
 */
@Mapper
public interface VideoCameraMapper extends BaseMapper<VideoCamera> {

    @Select({
            "<script>",
            "SELECT v.*, p.pole_name AS poleName",
            "FROM video_camera v",
            "LEFT JOIN dev_pole p ON v.pole_id = p.id",
            "WHERE v.deleted = 0",
            "<if test='query.cameraName != null and query.cameraName != \"\"'> AND v.camera_name LIKE CONCAT('%', #{query.cameraName}, '%') </if>",
            "<if test='query.status != null'> AND v.status = #{query.status} </if>",
            "ORDER BY v.create_time DESC",
            "</script>"
    })
    IPage<VideoCamera> selectVideoCameraPage(IPage<VideoCamera> page, @Param("query") VideoCameraQueryDTO query);

}
