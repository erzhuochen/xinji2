package com.xinji.service;

import com.xinji.dto.request.CounselorChatRequest;
import com.xinji.dto.response.CounselorChatResponse;
import com.xinji.dto.response.RecentDiariesResponse;

/**
 * AI心理咨询师服务接口
 */
public interface CounselorService {
    
    /**
     * 获取近七天的日记数量
     */
    RecentDiariesResponse getRecentDiariesCount(String userId);
    
    /**
     * 发送消息给AI心理咨询师
     */
    CounselorChatResponse chat(String userId, CounselorChatRequest request);
}






