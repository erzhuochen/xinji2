package com.xinji.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xinji.dto.response.AnalysisResponse;
import com.xinji.entity.Diary;
import com.xinji.entity.User;
import com.xinji.entity.mongo.AnalysisResult;
import com.xinji.entity.mongo.DiaryContent;
import com.xinji.exception.BusinessException;
import com.xinji.mapper.DiaryRepository;
import com.xinji.mapper.UserRepository;
import com.xinji.repository.mongo.AnalysisResultRepository;
import com.xinji.repository.mongo.DiaryContentRepository;
import com.xinji.service.AnalysisService;
import com.xinji.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AI分析服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {
    
    private final DiaryRepository diaryRepository;
    private final DiaryContentRepository diaryContentRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserRepository userRepository;
    private final AESUtil aesUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;
    
    @Value("${aliyun.dashscope.model:qwen-plus}")
    private String model;
    
    @Value("${ai-quota.free-daily-limit:5}")
    private int freeDailyLimit;
    
    @Value("${ai-quota.pro-daily-limit:1000}")
    private int proDailyLimit;
    
    private static final String AI_QUOTA_PREFIX = "ai:quota:";
    private static final String ANALYSIS_LOCK_PREFIX = "analysis:lock:";
    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    // 8类基本情绪
    private static final List<String> EMOTIONS = Arrays.asList(
            "HAPPY", "SAD", "ANGRY", "FEAR", "SURPRISE", "DISGUST", "NEUTRAL", "ANXIOUS"
    );
    
    // 认知偏差模式
    private static final Map<String, List<String>> COGNITIVE_DISTORTIONS = new LinkedHashMap<>();
    static {
        COGNITIVE_DISTORTIONS.put("CATASTROPHIZING", Arrays.asList("完蛋", "毁了", "再也"));
        COGNITIVE_DISTORTIONS.put("BLACK_WHITE", Arrays.asList("总是", "从不", "永远"));
        COGNITIVE_DISTORTIONS.put("OVERGENERALIZATION", Arrays.asList("每次", "所有人"));
        COGNITIVE_DISTORTIONS.put("MIND_READING", Arrays.asList("他肯定觉得", "她一定认为"));
        COGNITIVE_DISTORTIONS.put("EMOTIONAL_REASONING", Arrays.asList("我感觉我很失败"));
        COGNITIVE_DISTORTIONS.put("SHOULD_STATEMENTS", Arrays.asList("我应该", "必须"));
        COGNITIVE_DISTORTIONS.put("LABELING", Arrays.asList("我是个失败者", "我很笨"));
        COGNITIVE_DISTORTIONS.put("PERSONALIZATION", Arrays.asList("都是我的错"));
        COGNITIVE_DISTORTIONS.put("MENTAL_FILTER", Arrays.asList("只看到"));
        COGNITIVE_DISTORTIONS.put("DISQUALIFYING_POSITIVE", Arrays.asList("但那不算什么"));
    }
    
    @Override
    public AnalysisResponse submitAnalysis(String userId, String diaryId) {
        // 检查日记是否存在
        Diary diary = diaryRepository.selectById(diaryId);
        if (diary == null || diary.getDeleted() == 1) {
            throw BusinessException.notFound("日记不存在");
        }
        
        if (!diary.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权分析该日记");
        }
        
        // 检查AI配额
        checkAiQuota(userId);
        
        // 检查分析锁(同一日记2分钟内不能重复分析)
        String lockKey = ANALYSIS_LOCK_PREFIX + diaryId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw BusinessException.tooManyRequests("分析请求过于频繁，请2分钟后重试");
        }
        
        // 获取日记内容
        DiaryContent content = diaryContentRepository.findById(diaryId)
                .orElseThrow(() -> BusinessException.notFound("日记内容不存在"));
        String plainContent = aesUtil.decrypt(content.getContent());
        
        // 创建分析任务
        AnalysisResult result = new AnalysisResult();
        result.setId(UUID.randomUUID().toString().replace("-", ""));
        result.setDiaryId(diaryId);
        result.setUserId(userId);
        result.setStatus("PROCESSING");
        result.setCreatedAt(LocalDateTime.now());
        analysisResultRepository.save(result);
        
        // 设置分析锁
        redisTemplate.opsForValue().set(lockKey, "1", 2, TimeUnit.MINUTES);
        
        // 异步执行AI分析
        executeAnalysisAsync(result.getId(), plainContent, diary);
        
        // 增加配额使用
        incrementAiQuota(userId);
        
        log.info("AI分析任务已提交: analysisId={}, diaryId={}", result.getId(), diaryId);
        
        return convertToResponse(result);
    }
    
    @Override
    public AnalysisResponse getAnalysis(String userId, String analysisId) {
        AnalysisResult result = analysisResultRepository.findById(analysisId)
                .orElseThrow(() -> BusinessException.notFound("分析结果不存在"));
        
        if (!result.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权查看该分析结果");
        }
        
        return convertToResponse(result);
    }
    
    /**
     * 异步执行AI分析
     */
    @Async
    public void executeAnalysisAsync(String analysisId, String content, Diary diary) {
        try {
            // 调用AI进行情绪分析
            Map<String, Object> aiResult = callAiForAnalysis(content);
            
            // 更新分析结果
            AnalysisResult result = analysisResultRepository.findById(analysisId).orElse(null);
            if (result == null) {
                log.error("分析结果不存在: {}", analysisId);
                return;
            }
            
            result.setStatus("COMPLETED");
            result.setEmotions(extractEmotions(aiResult));
            result.setPrimaryEmotion(findPrimaryEmotion(result.getEmotions()));
            result.setEmotionIntensity(result.getEmotions().getOrDefault(result.getPrimaryEmotion(), 0.5));
            result.setKeywords(extractKeywords(aiResult));
            result.setCognitiveDistortions(detectCognitiveDistortions(content));
            result.setSuggestions(generateSuggestions(aiResult, result.getPrimaryEmotion()));
            result.setRiskLevel(assessRiskLevel(result));
            result.setAnalyzedAt(LocalDateTime.now());
            
            analysisResultRepository.save(result);
            
            // 更新日记的分析状态
            diary.setAnalyzed(1);
            diary.setAnalysisId(analysisId);
            diary.setPrimaryEmotion(result.getPrimaryEmotion());
            diary.setEmotionIntensity(result.getEmotionIntensity());
            diaryRepository.updateById(diary);
            
            log.info("AI分析完成: analysisId={}, emotion={}", analysisId, result.getPrimaryEmotion());
            
        } catch (Exception e) {
            log.error("AI分析失败: analysisId={}", analysisId, e);
            
            // 更新状态为失败
            AnalysisResult result = analysisResultRepository.findById(analysisId).orElse(null);
            if (result != null) {
                result.setStatus("FAILED");
                analysisResultRepository.save(result);
            }
        }
    }
    
    /**
     * 调用阿里云百炼AI进行分析
     */
    private Map<String, Object> callAiForAnalysis(String content) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            String prompt = buildAnalysisPrompt(content);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", "你是一个专业的心理咨询师，擅长情绪分析和认知行为疗法。请分析用户日记中的情绪状态，用JSON格式返回结果。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));
            
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
                return JSON.parseObject(resultContent);
            }
            
        } catch (Exception e) {
            log.error("调用AI API失败", e);
        }
        
        // 返回默认分析结果
        return generateDefaultAnalysis();
    }
    
    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String content) {
        return String.format("""
            请分析以下日记内容的情绪状态：
            
            ---
            %s
            ---
            
            请返回JSON格式的分析结果，包含以下字段：
            {
                "emotions": {
                    "HAPPY": 0.0-1.0,
                    "SAD": 0.0-1.0,
                    "ANGRY": 0.0-1.0,
                    "FEAR": 0.0-1.0,
                    "SURPRISE": 0.0-1.0,
                    "DISGUST": 0.0-1.0,
                    "NEUTRAL": 0.0-1.0,
                    "ANXIOUS": 0.0-1.0
                },
                "keywords": ["关键词1", "关键词2", ...],
                "suggestion": "调节建议文字"
            }
            """, content.length() > 2000 ? content.substring(0, 2000) : content);
    }
    
    /**
     * 生成默认分析结果(AI调用失败时使用)
     */
    private Map<String, Object> generateDefaultAnalysis() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Double> emotions = new HashMap<>();
        emotions.put("NEUTRAL", 0.5);
        for (String emotion : EMOTIONS) {
            if (!emotion.equals("NEUTRAL")) {
                emotions.put(emotion, 0.1);
            }
        }
        result.put("emotions", emotions);
        result.put("keywords", Arrays.asList("日记", "记录"));
        result.put("suggestion", "建议保持写日记的好习惯，记录生活中的点滴感受");
        return result;
    }
    
    /**
     * 提取情绪分布
     */
    @SuppressWarnings("unchecked")
    private Map<String, Double> extractEmotions(Map<String, Object> aiResult) {
        Map<String, Double> emotions = new HashMap<>();
        
        if (aiResult.containsKey("emotions")) {
            Map<String, Object> rawEmotions = (Map<String, Object>) aiResult.get("emotions");
            for (String emotion : EMOTIONS) {
                Object value = rawEmotions.get(emotion);
                if (value instanceof Number) {
                    emotions.put(emotion, ((Number) value).doubleValue());
                } else {
                    emotions.put(emotion, 0.0);
                }
            }
        } else {
            // 默认情绪分布
            for (String emotion : EMOTIONS) {
                emotions.put(emotion, emotion.equals("NEUTRAL") ? 0.5 : 0.1);
            }
        }
        
        return emotions;
    }
    
    /**
     * 找出主导情绪
     */
    private String findPrimaryEmotion(Map<String, Double> emotions) {
        return emotions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NEUTRAL");
    }
    
    /**
     * 提取关键词
     */
    @SuppressWarnings("unchecked")
    private List<String> extractKeywords(Map<String, Object> aiResult) {
        if (aiResult.containsKey("keywords")) {
            Object keywords = aiResult.get("keywords");
            if (keywords instanceof List) {
                List<String> result = new ArrayList<>();
                for (Object kw : (List<?>) keywords) {
                    result.add(kw.toString());
                    if (result.size() >= 10) break;
                }
                return result;
            }
        }
        return Collections.emptyList();
    }
    
    /**
     * 检测认知偏差
     */
    private List<AnalysisResult.CognitiveDistortion> detectCognitiveDistortions(String content) {
        List<AnalysisResult.CognitiveDistortion> distortions = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : COGNITIVE_DISTORTIONS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (content.contains(keyword)) {
                    AnalysisResult.CognitiveDistortion distortion = new AnalysisResult.CognitiveDistortion();
                    distortion.setType(entry.getKey());
                    distortion.setDescription(getDistortionDescription(entry.getKey()));
                    distortions.add(distortion);
                    break;
                }
            }
        }
        
        if (distortions.isEmpty()) {
            AnalysisResult.CognitiveDistortion none = new AnalysisResult.CognitiveDistortion();
            none.setType("NONE");
            none.setDescription("未检测到明显认知偏差");
            distortions.add(none);
        }
        
        return distortions;
    }
    
    /**
     * 获取认知偏差描述
     */
    private String getDistortionDescription(String type) {
        return switch (type) {
            case "CATASTROPHIZING" -> "灾难化思维 - 倾向于将事情往最坏的方向想";
            case "BLACK_WHITE" -> "非黑即白思维 - 用极端的方式看待事物";
            case "OVERGENERALIZATION" -> "过度概括 - 从单一事件得出普遍结论";
            case "MIND_READING" -> "读心术 - 假设知道别人在想什么";
            case "EMOTIONAL_REASONING" -> "情绪推理 - 把感受当作事实";
            case "SHOULD_STATEMENTS" -> "应该句式 - 对自己有过高要求";
            case "LABELING" -> "贴标签 - 给自己或他人贴负面标签";
            case "PERSONALIZATION" -> "个人化 - 将不相关的事归咎于自己";
            case "MENTAL_FILTER" -> "心理过滤 - 只关注负面信息";
            case "DISQUALIFYING_POSITIVE" -> "否定正面 - 忽视积极的一面";
            default -> "未知认知偏差";
        };
    }
    
    /**
     * 生成调节建议
     */
    private List<String> generateSuggestions(Map<String, Object> aiResult, String primaryEmotion) {
        List<String> suggestions = new ArrayList<>();
        
        // 从AI结果中提取建议
        if (aiResult.containsKey("suggestion")) {
            suggestions.add(aiResult.get("suggestion").toString());
        }
        
        // 根据主导情绪添加建议
        switch (primaryEmotion) {
            case "SAD" -> {
                suggestions.add("试着做一些让自己开心的事情，比如听音乐、散步");
                suggestions.add("与信任的朋友或家人分享你的感受");
            }
            case "ANXIOUS" -> {
                suggestions.add("尝试深呼吸练习，每次吸气4秒，屏气4秒，呼气4秒");
                suggestions.add("写下让你焦虑的具体事项，逐一分析其可能性");
            }
            case "ANGRY" -> {
                suggestions.add("先让自己冷静下来，离开让你生气的环境");
                suggestions.add("试着从对方的角度思考问题");
            }
            case "FEAR" -> {
                suggestions.add("识别恐惧的来源，问问自己这种恐惧是否合理");
                suggestions.add("尝试渐进式暴露疗法，逐步面对恐惧");
            }
            case "HAPPY" -> {
                suggestions.add("记录下让你快乐的具体事件，建立积极回忆库");
                suggestions.add("与他人分享你的快乐，传递正能量");
            }
            default -> suggestions.add("继续保持写日记的习惯，记录每天的感受和想法");
        }
        
        return suggestions;
    }
    
    /**
     * 评估风险等级
     */
    private String assessRiskLevel(AnalysisResult result) {
        // 检查是否有高风险关键词
        List<String> highRiskKeywords = Arrays.asList("自杀", "不想活", "结束生命", "死", "遗书");
        for (String keyword : result.getKeywords()) {
            if (highRiskKeywords.stream().anyMatch(keyword::contains)) {
                return "HIGH";
            }
        }
        
        // 检查负面情绪强度
        Double sadIntensity = result.getEmotions().getOrDefault("SAD", 0.0);
        Double anxiousIntensity = result.getEmotions().getOrDefault("ANXIOUS", 0.0);
        Double fearIntensity = result.getEmotions().getOrDefault("FEAR", 0.0);
        
        double negativeSum = sadIntensity + anxiousIntensity + fearIntensity;
        
        if (negativeSum > 2.0) {
            return "HIGH";
        } else if (negativeSum > 1.5) {
            return "MEDIUM";
        }
        
        return "LOW";
    }
    
    /**
     * 检查AI配额
     */
    private void checkAiQuota(String userId) {
        User user = userRepository.selectById(userId);
        boolean isPro = user != null && "PRO".equals(user.getMemberStatus());
        int limit = isPro ? proDailyLimit : freeDailyLimit;
        
        String key = AI_QUOTA_PREFIX + userId;
        Object count = redisTemplate.opsForValue().get(key);
        int used = count == null ? 0 : Integer.parseInt(count.toString());
        
        if (used >= limit) {
            throw BusinessException.forbidden("今日AI分析次数已用完，" + (isPro ? "请明天再试" : "升级Pro会员可获得更多次数"));
        }
    }
    
    /**
     * 增加AI配额使用
     */
    private void incrementAiQuota(String userId) {
        String key = AI_QUOTA_PREFIX + userId;
        redisTemplate.opsForValue().increment(key);
        // 设置过期时间到当天结束
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }
    
    /**
     * 转换为响应对象
     */
    private AnalysisResponse convertToResponse(AnalysisResult result) {
        AnalysisResponse response = new AnalysisResponse();
        response.setId(result.getId());
        response.setDiaryId(result.getDiaryId());
        response.setStatus(result.getStatus());
        response.setEmotions(result.getEmotions());
        response.setPrimaryEmotion(result.getPrimaryEmotion());
        response.setEmotionIntensity(result.getEmotionIntensity());
        response.setKeywords(result.getKeywords());
        response.setRiskLevel(result.getRiskLevel());
        response.setSuggestions(result.getSuggestions());
        response.setAnalyzedAt(result.getAnalyzedAt());
        
        if (result.getCognitiveDistortions() != null) {
            List<AnalysisResponse.CognitiveDistortion> distortions = result.getCognitiveDistortions().stream()
                    .map(d -> {
                        AnalysisResponse.CognitiveDistortion dto = new AnalysisResponse.CognitiveDistortion();
                        dto.setType(d.getType());
                        dto.setDescription(d.getDescription());
                        return dto;
                    })
                    .toList();
            response.setCognitiveDistortions(distortions);
        }
        
        return response;
    }
}
