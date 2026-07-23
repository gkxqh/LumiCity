package com.ccb.lighting.module.publish.dto;

import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LedProgramQueryDTO extends PageQuery {
    private String programName;
    private String mediaType;
    private Integer status;
}
