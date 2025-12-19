package com.xinji.service;

import com.xinji.dto.request.DiaryRequest;
import com.xinji.dto.response.DiaryResponse;
import com.xinji.dto.response.PageResponse;

import java.time.LocalDate;

/**
 * 日记服务接口
 */
public interface DiaryService {
    
    /**
     * 创建日记
     */
    DiaryResponse create(String userId, DiaryRequest request);
    
    /**
     * 获取日记列表
     */
    PageResponse<DiaryResponse> list(String userId, Integer page, Integer pageSize, 
                                      LocalDate startDate, LocalDate endDate, String keyword);
    
    /**
     * 获取日记详情
     */
    DiaryResponse getById(String userId, String diaryId);
    
    /**
     * 更新日记
     */
    DiaryResponse update(String userId, String diaryId, DiaryRequest request);
    
    /**
     * 删除日记
     */
    void delete(String userId, String diaryId);
}
