package com.ccb.lighting.module.alarm.dto;


import com.ccb.lighting.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AlarmQueryDTO extends PageQuery {
    private String alarmType;
    private Integer alarmLevel;
    private Integer status;
}
