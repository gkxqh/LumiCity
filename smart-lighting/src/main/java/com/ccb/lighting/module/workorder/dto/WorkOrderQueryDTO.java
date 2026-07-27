package com.ccb.lighting.module.workorder.dto;

import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderQueryDTO extends PageQuery {

    private String orderType;

    private Integer status;

    private String deviceId;

    private Integer alarmId;


}
