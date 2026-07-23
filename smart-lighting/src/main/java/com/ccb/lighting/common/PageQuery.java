package com.ccb.lighting.common;

import lombok.Data;

/**
 * 分页查询基类
 * 所有分页查询 DTO 继承它，自动拥有 current/pageNum 和 size/pageSize
 * Controller 接收时：?current=1&size=10
 */
@Data
public class PageQuery {
    private Integer current = 1;   // 当前页，默认第 1 页
    private Integer size = 10;     // 每页条数，默认 10
}
