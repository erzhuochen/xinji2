package com.xinji.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xinji.dto.request.CounselorChatRequest;
import com.xinji.dto.response.CounselorChatResponse;
import com.xinji.dto.response.RecentDiariesResponse;
import com.xinji.entity.Diary;
import com.xinji.entity.mongo.DiaryContent;
import com.xinji.mapper.DiaryRepository;
import com.xinji.repository.mongo.DiaryContentRepository;
import com.xinji.service.CounselorService;
import com.xinji.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI心理咨询师服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CounselorServiceImpl implements CounselorService {
    
    private final DiaryRepository diaryRepository;
    private final DiaryContentRepository diaryContentRepository;
    private final AESUtil aesUtil;
    
    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;
    
    @Value("${aliyun.dashscope.model:qwen-plus}")
    private String model;
    
    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    @Override
    public RecentDiariesResponse getRecentDiariesCount(String userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 近7天（包含今天）
        
        List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        // 只统计非草稿的日记
        int count = (int) diaries.stream()
                .filter(d -> d.getIsDraft() == 0)
                .count();
        
        RecentDiariesResponse response = new RecentDiariesResponse();
        response.setCount(count);
        return response;
    }
    
    @Override
    public CounselorChatResponse chat(String userId, CounselorChatRequest request) {
        // 获取近七天的日记内容
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        
        List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        List<Diary> validDiaries = diaries.stream()
                .filter(d -> d.getIsDraft() == 0)
                .sorted(Comparator.comparing(Diary::getDiaryDate).reversed())
                .collect(Collectors.toList());
        
        // 获取日记内容
        List<String> diaryIds = validDiaries.stream()
                .map(Diary::getId)
                .collect(Collectors.toList());
        
        List<DiaryContent> contents = diaryContentRepository.findAllById(diaryIds);
        Map<String, DiaryContent> contentMap = contents.stream()
                .collect(Collectors.toMap(DiaryContent::getId, c -> c));
        
        // 构建日记上下文（最多最近7篇，每篇最多500字）
        StringBuilder diaryContext = new StringBuilder();
        int diaryCount = 0;
        for (Diary diary : validDiaries) {
            if (diaryCount >= 7) break;
            
            DiaryContent content = contentMap.get(diary.getId());
            if (content != null) {
                String plainContent = aesUtil.decrypt(content.getContent());
                String truncatedContent = plainContent.length() > 500 
                        ? plainContent.substring(0, 500) + "..." 
                        : plainContent;
                
                diaryContext.append(String.format("\n【%s】%s\n%s\n", 
                        diary.getDiaryDate(), 
                        diary.getTitle() != null ? diary.getTitle() : "无标题",
                        truncatedContent));
                diaryCount++;
            }
        }
        
        // 调用AI API
        String aiReply = callAiForCounseling(request.getMessage(), diaryContext.toString(), diaryCount);
        
        CounselorChatResponse response = new CounselorChatResponse();
        response.setReply(aiReply);
        return response;
    }
    
    /**
     * 调用AI进行心理咨询对话
     */
    private String callAiForCounseling(String userMessage, String diaryContext, int diaryCount) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(diaryCount);
            
            // 构建用户消息（包含日记上下文）
            String userPrompt = buildUserPrompt(userMessage, diaryContext, diaryCount);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    DASHSCOPE_API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                String resultContent = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                return resultContent;
            }
            
        } catch (Exception e) {
            log.error("调用AI API失败", e);
        }
        
        return "抱歉，我暂时无法回答您的问题，请稍后重试。";
    }
    
    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(int diaryCount) {
        return String.format("""
            你是一位专业的心理咨询师，擅长认知行为疗法（CBT）和接纳承诺疗法（ACT）。
            
            用户已经提供了近七天的日记记录（共%d篇），这些日记反映了用户最近的情绪状态、生活事件和内心想法。
            
            你的任务是：
            1. 基于用户的日记内容，理解用户的情绪状态和困扰
            2. 提供专业、温暖、共情的心理支持和建议
            3. 使用认知行为疗法帮助用户识别和调整负面思维
            4. 使用接纳承诺疗法帮助用户接纳情绪，关注当下
            5. 提供具体、可操作的建议和练习
            
            回答要求：
            - 如果用户的问题没有涉及日记内容，请直接回答用户问题，忽略以下要求和日记
            - 语气温暖、专业、共情
            - 避免使用过于专业的术语，用通俗易懂的语言
            - 回答要具体、有针对性，不要泛泛而谈
            - 如果日记中提到了具体问题，要针对性地回应
            - 回答长度控制在200-500字之间
            """, diaryCount);
    }
    
    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(String userMessage, String diaryContext, int diaryCount) {
        if (diaryCount > 0) {
            return String.format("""
                我的对话是：%s
                如果我的对话内容没有涉及日记内容，请直接回答我的问题，忽略以下要求和日记
                以下是我近七天的日记记录：
                %s
                
                
                
                请根据我的日记内容，给我一些专业的心理支持和建议。
                """, userMessage, diaryContext);
        } else {
            return String.format("""
                我近七天还没有日记记录，但我想和你聊聊：%s
                
                请给我一些专业的心理支持和建议。
                """, userMessage);
        }
    }
}

