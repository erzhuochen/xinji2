package com.xinji.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 分页响应DTO
 */
@Data
public class PageResponse<T> {
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页数量
     */
    private Integer pageSize;
    
    /**
     * 数据列表
     */
    private List<T> list;
    
    public PageResponse(Long total, Integer page, Integer pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }
}
