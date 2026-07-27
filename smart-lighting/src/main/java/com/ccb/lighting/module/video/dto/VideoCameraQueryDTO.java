package com.ccb.lighting.module.video.dto;

import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VideoCameraQueryDTO extends PageQuery {
    private String cameraName;
    private Integer status;
    private String poleName;
}
