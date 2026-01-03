package com.xinji.service.impl;

import com.xinji.dto.response.InsightsReportResponse;
import com.xinji.dto.response.WeeklyReportResponse;
import com.xinji.entity.Diary;
import com.xinji.entity.User;
import com.xinji.entity.mongo.AnalysisResult;
import com.xinji.entity.mongo.WeeklyReport;
import com.xinji.exception.BusinessException;
import com.xinji.mapper.DiaryRepository;
import com.xinji.mapper.UserRepository;
import com.xinji.repository.mongo.AnalysisResultRepository;
import com.xinji.repository.mongo.WeeklyReportRepository;
import com.xinji.service.AnalysisService;
import com.xinji.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 报告服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    
    private final DiaryRepository diaryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AnalysisService analysisService;
    
    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;
    
    @Value("${aliyun.dashscope.model:qwen-plus}")
    private String model;
    
    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    private static final String WEEKLY_REPORT_CACHE_PREFIX = "report:weekly:";
    
    @Override
    public WeeklyReportResponse getWeeklyReport(String userId, LocalDate startDate) {
        // 计算周的起止日期
        if (startDate == null) {
            startDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else {
            // 确保是周一
            startDate = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        LocalDate endDate = startDate.plusDays(6);
        
        // 尝试从缓存获取
        String cacheKey = WEEKLY_REPORT_CACHE_PREFIX + userId + ":" + startDate;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof WeeklyReportResponse) {
//            return (WeeklyReportResponse) cached;
        }
        
        // 查询该周的日记
        List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        // 获取已分析的日记ID
        List<String> analyzedDiaryIds = diaries.stream()
                .filter(d -> d.getAnalyzed() == 1)
                .map(Diary::getId)
                .collect(Collectors.toList());
        
        // 获取分析结果
        List<AnalysisResult> analysisResults = analyzedDiaryIds.isEmpty() ? 
                Collections.emptyList() : 
                analysisResultRepository.findByDiaryIdIn(analyzedDiaryIds);
        
        // 构建周报数据
        WeeklyReportResponse response = buildWeeklyReport(startDate, endDate, diaries, analysisResults);
        
        // 缓存结果(7天)
        redisTemplate.opsForValue().set(cacheKey, response, 7, TimeUnit.DAYS);
        
        return response;
    }
    
    @Override
    public InsightsReportResponse getInsightsReport(String userId, String timeRange) {
        // 验证Pro权限
        User user = userRepository.selectById(userId);
        if (user == null || !"PRO".equals(user.getMemberStatus())) {
            throw BusinessException.forbidden("此功能仅限Pro会员使用");
        }
        
        // 计算时间范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        
        switch (timeRange == null ? "month" : timeRange.toLowerCase()) {
            case "week" -> startDate = endDate.minusWeeks(1);
            case "all" -> startDate = endDate.minusYears(1);
            default -> startDate = endDate.minusMonths(1);
        }
        
        // 获取该时间段的分析结果
        List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        List<String> diaryIds = diaries.stream().map(Diary::getId).collect(Collectors.toList());
        List<AnalysisResult> analysisResults = diaryIds.isEmpty() ? 
                Collections.emptyList() : 
                analysisResultRepository.findByDiaryIdIn(diaryIds);
        
        // 构建深度洞察报告
        InsightsReportResponse response = buildInsightsReport(startDate, endDate, analysisResults);
        
        return response;
    }
    
    @Override
    public void generateWeeklyReports() {
        log.info("开始生成用户周报...");
        
        // 获取上周的日期范围
        LocalDate lastMonday = LocalDate.now().minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastSunday = lastMonday.plusDays(6);
        
        // 获取所有有日记的用户ID
        // TODO: 分页处理大量用户
        List<User> users = userRepository.selectList(null);
        
        int successCount = 0;
        int failCount = 0;
        
        for (User user : users) {
            try {
                // 查询用户上周的日记
                List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(user.getId(), lastMonday, lastSunday);
                
                if (diaries.isEmpty()) {
                    continue;
                }
                
                // 获取分析结果
                List<String> diaryIds = diaries.stream()
                        .filter(d -> d.getAnalyzed() == 1)
                        .map(Diary::getId)
                        .collect(Collectors.toList());
                
                List<AnalysisResult> analysisResults = diaryIds.isEmpty() ? 
                        Collections.emptyList() : 
                        analysisResultRepository.findByDiaryIdIn(diaryIds);
                
                // 生成并保存周报
                WeeklyReport report = new WeeklyReport();
                report.setId(UUID.randomUUID().toString().replace("-", ""));
                report.setUserId(user.getId());
                report.setWeekStart(lastMonday);
                report.setWeekEnd(lastSunday);
                report.setDiaryCount(diaries.size());
                report.setAnalyzedCount(analysisResults.size());
                
                // 计算情绪统计
                Map<String, Integer> emotionDistribution = new HashMap<>();
                List<WeeklyReport.EmotionTrend> emotionTrend = new ArrayList<>();
                double totalIntensity = 0;
                
                for (AnalysisResult ar : analysisResults) {
                    String emotion = ar.getPrimaryEmotion();
                    emotionDistribution.merge(emotion, 1, Integer::sum);
                    totalIntensity += ar.getEmotionIntensity();
                    
                    // 查找对应日记的日期
                    Diary diary = diaries.stream()
                            .filter(d -> d.getId().equals(ar.getDiaryId()))
                            .findFirst()
                            .orElse(null);
                    
                    if (diary != null) {
                        WeeklyReport.EmotionTrend trend = new WeeklyReport.EmotionTrend();
                        trend.setDate(diary.getDiaryDate());
                        trend.setEmotion(emotion);
                        trend.setIntensity(ar.getEmotionIntensity());
                        emotionTrend.add(trend);
                    }
                }
                
                report.setEmotionDistribution(emotionDistribution);
                report.setEmotionTrend(emotionTrend.stream()
                        .sorted(Comparator.comparing(WeeklyReport.EmotionTrend::getDate))
                        .collect(Collectors.toList()));
                
                if (!analysisResults.isEmpty()) {
                    report.setAverageIntensity(totalIntensity / analysisResults.size());
                    report.setMostFrequentEmotion(emotionDistribution.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("NEUTRAL"));
                }
                
                // 提取关键词
                List<String> allKeywords = analysisResults.stream()
                        .flatMap(ar -> ar.getKeywords() != null ? ar.getKeywords().stream() : java.util.stream.Stream.empty())
                        .filter(k -> k != null)
                        .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                        .entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(10)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                report.setKeywords(allKeywords);
                
                // 生成总结
                report.setSummary(generateWeeklySummary(report));
                report.setCreatedAt(LocalDateTime.now());
                
                weeklyReportRepository.save(report);
                successCount++;
                
            } catch (Exception e) {
                log.error("生成用户周报失败: userId={}", user.getId(), e);
                failCount++;
            }
        }
        
        log.info("周报生成完成: 成功={}, 失败={}", successCount, failCount);
    }
    
    /**
     * 构建周报响应
     */
    private WeeklyReportResponse buildWeeklyReport(LocalDate startDate, LocalDate endDate,
                                                    List<Diary> diaries, List<AnalysisResult> analysisResults) {
        WeeklyReportResponse response = new WeeklyReportResponse();
        response.setWeekStart(startDate);
        response.setWeekEnd(endDate);
        response.setDiaryCount(diaries.size());
        response.setAnalyzedCount((int) diaries.stream().filter(d -> d.getAnalyzed() == 1).count());
        
        // 情绪趋势
        List<WeeklyReportResponse.EmotionTrendItem> emotionTrend = new ArrayList<>();
        Map<String, Integer> emotionDistribution = new HashMap<>();
        double totalIntensity = 0;
        
        Map<String, AnalysisResult> resultMap = analysisResults.stream()
                .collect(Collectors.toMap(AnalysisResult::getDiaryId, ar -> ar, (a, b) -> a));
        
        for (Diary diary : diaries) {
            if (diary.getAnalyzed() == 1 && resultMap.containsKey(diary.getId())) {
                AnalysisResult ar = resultMap.get(diary.getId());
                
                // 跳过没有情绪分析结果的记录
                if (ar.getPrimaryEmotion() == null) {
                    continue;
                }
                
                WeeklyReportResponse.EmotionTrendItem item = new WeeklyReportResponse.EmotionTrendItem();
                item.setDate(diary.getDiaryDate());
                item.setEmotion(ar.getPrimaryEmotion());
                item.setIntensity(ar.getEmotionIntensity() != null ? ar.getEmotionIntensity() : 0.5);
                emotionTrend.add(item);
                
                emotionDistribution.merge(ar.getPrimaryEmotion(), 1, Integer::sum);
                if (ar.getEmotionIntensity() != null) {
                    totalIntensity += ar.getEmotionIntensity();
                }
            }
        }
        
        // 按日期排序
        emotionTrend.sort(Comparator.comparing(WeeklyReportResponse.EmotionTrendItem::getDate));
        response.setEmotionTrend(emotionTrend);
        response.setEmotionDistribution(emotionDistribution);
        
        if (!analysisResults.isEmpty()) {
            response.setAverageIntensity(totalIntensity / analysisResults.size());
            response.setMostFrequentEmotion(emotionDistribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("NEUTRAL"));
        }
        
        // 关键词统计
        List<String> keywords = analysisResults.stream()
                .flatMap(ar -> ar.getKeywords() != null ? ar.getKeywords().stream() : java.util.stream.Stream.empty())
                .filter(k -> k != null)
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        response.setKeywords(keywords);
        
        // 生成总结
        response.setSummary(generateWeeklySummaryFromResponse(response));
        
        return response;
    }
    
    /**
     * 构建深度洞察报告
     */
    private InsightsReportResponse buildInsightsReport(LocalDate startDate, LocalDate endDate,
                                                        List<AnalysisResult> analysisResults) {
        InsightsReportResponse response = new InsightsReportResponse();
        response.setTimeRange("month");
        response.setStartDate(startDate.format(DateTimeFormatter.ISO_DATE));
        response.setEndDate(endDate.format(DateTimeFormatter.ISO_DATE));
        
        if (analysisResults.isEmpty()) {
            response.setInsights(Collections.emptyList());
            response.setGrowthPlan(Arrays.asList("开始记录日记，了解自己的情绪状态"));
            return response;
        }
        
        // 调用 AI 生成深度洞察
        String aiInsights = generateAiInsights(startDate, endDate, analysisResults);
        
        // 解析 AI 返回的洞察内容
        List<InsightsReportResponse.Insight> insights = parseAiInsights(aiInsights, analysisResults);
        response.setInsights(insights);
        
        // 生成成长计划（也通过 AI）
        List<String> growthPlan = generateGrowthPlan(analysisResults, aiInsights);
        response.setGrowthPlan(growthPlan);
        
        // 情绪预测
        InsightsReportResponse.EmotionForecast forecast = generateEmotionForecast(analysisResults);
        response.setEmotionForecast(forecast);
        
        // 正念练习推荐
        List<InsightsReportResponse.MindfulnessSuggestion> mindfulness = generateMindfulnessSuggestions(analysisResults);
        response.setMindfulnessSuggestions(mindfulness);
        
        return response;
    }
    
    /**
     * 调用 AI 生成深度洞察
     */
    private String generateAiInsights(LocalDate startDate, LocalDate endDate, List<AnalysisResult> analysisResults) {
        // 统计情绪数据
        Map<String, Long> emotionCounts = analysisResults.stream()
                .filter(ar -> ar.getPrimaryEmotion() != null)
                .collect(Collectors.groupingBy(AnalysisResult::getPrimaryEmotion, Collectors.counting()));
        
        // 统计认知偏差
        Map<String, Long> distortionCounts = analysisResults.stream()
                .flatMap(ar -> ar.getCognitiveDistortions() != null ? ar.getCognitiveDistortions().stream() : java.util.stream.Stream.empty())
                .filter(cd -> cd.getType() != null && !"NONE".equals(cd.getType()))
                .collect(Collectors.groupingBy(AnalysisResult.CognitiveDistortion::getType, Collectors.counting()));
        
        // 提取关键情绪事件
        List<String> emotionalEvents = analysisResults.stream()
                .filter(ar -> ar.getEmotionIntensity() != null && ar.getEmotionIntensity() > 0.7)
                .limit(5)
                .map(ar -> translateEmotion(ar.getPrimaryEmotion()) + "(强度:" + String.format("%.1f", ar.getEmotionIntensity()) + ")")
                .collect(Collectors.toList());
        
        // 构建 AI prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的心理咨询师。请基于以下用户的情绪数据，提供深度心理洞察和专业建议。\n\n");
        prompt.append("【时间范围】").append(startDate).append(" 至 ").append(endDate).append("\n");
        prompt.append("【日记数量】").append(analysisResults.size()).append(" 篇\n\n");
        
        prompt.append("【情绪分布】\n");
        emotionCounts.forEach((emotion, count) -> 
            prompt.append("- ").append(translateEmotion(emotion)).append(": ").append(count).append("次\n")
        );
        
        if (!emotionalEvents.isEmpty()) {
            prompt.append("\n【高强度情绪事件】\n");
            emotionalEvents.forEach(event -> prompt.append("- ").append(event).append("\n"));
        }
        
        if (!distortionCounts.isEmpty()) {
            prompt.append("\n【检测到的认知偏差】\n");
            distortionCounts.forEach((type, count) -> 
                prompt.append("- ").append(translateDistortion(type)).append(": ").append(count).append("次\n")
            );
        }
        
        prompt.append("\n请提供以下内容（用 JSON 格式返回）：\n");
        prompt.append("1. insights: 3-5条深度心理洞察（每条包含 type、title、content、confidence）\n");
        prompt.append("   - type 可以是: EMOTION_PATTERN（情绪模式）、COGNITIVE_PATTERN（认知模式）、BEHAVIOR_PATTERN（行为模式）、GROWTH_OPPORTUNITY（成长机会）\n");
        prompt.append("   - title: 洞察标题（简短）\n");
        prompt.append("   - content: 详细分析和建议（100-200字）\n");
        prompt.append("   - confidence: 置信度（0-1之间）\n");
        prompt.append("2. growthPlan: 3-5条具体可行的成长建议\n");
        prompt.append("3. emotionForecast: 情绪风险预测（包含 nextWeekRisk 和 triggers）\n");
        prompt.append("   - nextWeekRisk: LOW/MEDIUM/HIGH\n");
        prompt.append("   - triggers: 可能的触发因素数组\n\n");
        prompt.append("要求：\n");
        prompt.append("- 洞察要专业、深入，不要泛泛而谈\n");
        prompt.append("- 建议要具体、可操作，避免空洞的鸡汤\n");
        prompt.append("- 语气温和、支持性，避免说教\n");
        prompt.append("- 返回格式必须是有效的 JSON\n");
        
        try {
            return callAiApi(prompt.toString());
        } catch (Exception e) {
            log.error("AI 生成洞察失败", e);
            return generateFallbackInsights(emotionCounts, distortionCounts);
        }
    }
    
    /**
     * 调用阿里云通义千问 API
     */
    private String callAiApi(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", "你是一个专业的心理咨询师，擅长情绪分析和认知行为疗法。请用JSON格式返回分析结果。"),
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
                com.alibaba.fastjson2.JSONObject jsonResponse = com.alibaba.fastjson2.JSON.parseObject(response.getBody());
                String resultContent = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                return resultContent;
            }
            
            throw new RuntimeException("AI API 调用失败");
        } catch (Exception e) {
            log.error("调用 AI API 失败", e);
            throw new RuntimeException("AI API 调用失败", e);
        }
    }
    
    /**
     * 解析 AI 返回的洞察内容
     */
    private List<InsightsReportResponse.Insight> parseAiInsights(String aiResponse, List<AnalysisResult> analysisResults) {
        try {
            // 尝试解析 JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(aiResponse);
            
            List<InsightsReportResponse.Insight> insights = new ArrayList<>();
            com.fasterxml.jackson.databind.JsonNode insightsNode = root.get("insights");
            
            if (insightsNode != null && insightsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode node : insightsNode) {
                    InsightsReportResponse.Insight insight = new InsightsReportResponse.Insight();
                    insight.setType(node.get("type").asText());
                    insight.setTitle(node.get("title").asText());
                    insight.setContent(node.get("content").asText());
                    insight.setConfidence(node.get("confidence").asDouble());
                    insights.add(insight);
                }
            }
            
            return insights.isEmpty() ? generateFallbackInsightsList(analysisResults) : insights;
        } catch (Exception e) {
            log.error("解析 AI 洞察失败", e);
            return generateFallbackInsightsList(analysisResults);
        }
    }
    
    /**
     * 生成成长计划
     */
    private List<String> generateGrowthPlan(List<AnalysisResult> analysisResults, String aiResponse) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(aiResponse);
            com.fasterxml.jackson.databind.JsonNode planNode = root.get("growthPlan");
            
            if (planNode != null && planNode.isArray()) {
                List<String> plan = new ArrayList<>();
                planNode.forEach(node -> plan.add(node.asText()));
                return plan;
            }
        } catch (Exception e) {
            log.error("解析成长计划失败", e);
        }
        
        return Arrays.asList(
                "建议每天进行10分钟正念冥想练习",
                "尝试记录每天的3件积极事件",
                "关注情绪触发因素，提升自我觉察能力"
        );
    }
    
    /**
     * 生成情绪预测
     */
    private InsightsReportResponse.EmotionForecast generateEmotionForecast(List<AnalysisResult> analysisResults) {
        InsightsReportResponse.EmotionForecast forecast = new InsightsReportResponse.EmotionForecast();
        
        // 计算负面情绪比例
        long negativeCount = analysisResults.stream()
                .filter(ar -> {
                    String emotion = ar.getPrimaryEmotion();
                    return emotion != null && (emotion.equals("ANXIETY") || emotion.equals("SADNESS") || 
                           emotion.equals("ANGER") || emotion.equals("FEAR"));
                })
                .count();
        
        double negativeRatio = analysisResults.isEmpty() ? 0 : (double) negativeCount / analysisResults.size();
        
        if (negativeRatio > 0.6) {
            forecast.setNextWeekRisk("HIGH");
        } else if (negativeRatio > 0.3) {
            forecast.setNextWeekRisk("MEDIUM");
        } else {
            forecast.setNextWeekRisk("LOW");
        }
        
        // 提取常见触发因素
        List<String> triggers = analysisResults.stream()
                .flatMap(ar -> ar.getKeywords() != null ? ar.getKeywords().stream() : java.util.stream.Stream.empty())
                .filter(k -> k != null)
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        forecast.setTriggers(triggers.isEmpty() ? Arrays.asList("工作压力", "人际关系") : triggers);
        
        return forecast;
    }
    
    /**
     * 生成正念练习推荐
     */
    private List<InsightsReportResponse.MindfulnessSuggestion> generateMindfulnessSuggestions(List<AnalysisResult> analysisResults) {
        List<InsightsReportResponse.MindfulnessSuggestion> suggestions = new ArrayList<>();
        
        // 基于主要情绪推荐练习
        Map<String, Long> emotionCounts = analysisResults.stream()
                .filter(ar -> ar.getPrimaryEmotion() != null)
                .collect(Collectors.groupingBy(AnalysisResult::getPrimaryEmotion, Collectors.counting()));
        
        String dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NEUTRAL");
        
        InsightsReportResponse.MindfulnessSuggestion breathing = new InsightsReportResponse.MindfulnessSuggestion();
        breathing.setTitle("呼吸冥想");
        breathing.setDuration("10分钟");
        breathing.setUrl("/mental-training/breathing");
        suggestions.add(breathing);
        
        if ("ANXIETY".equals(dominantEmotion) || "FEAR".equals(dominantEmotion)) {
            InsightsReportResponse.MindfulnessSuggestion cognitive = new InsightsReportResponse.MindfulnessSuggestion();
            cognitive.setTitle("认知重构练习");
            cognitive.setDuration("15分钟");
            cognitive.setUrl("/mental-training/cognitive-reframe");
            suggestions.add(cognitive);
        } else if ("SADNESS".equals(dominantEmotion)) {
            InsightsReportResponse.MindfulnessSuggestion gratitude = new InsightsReportResponse.MindfulnessSuggestion();
            gratitude.setTitle("感恩练习");
            gratitude.setDuration("10分钟");
            gratitude.setUrl("/mental-training/gratitude");
            suggestions.add(gratitude);
        }
        
        return suggestions;
    }
    
    /**
     * 生成备用洞察（AI 失败时）
     */
    private String generateFallbackInsights(Map<String, Long> emotionCounts, Map<String, Long> distortionCounts) {
        StringBuilder json = new StringBuilder();
        json.append("{\"insights\":[");
        
        if (!emotionCounts.isEmpty()) {
            String dominantEmotion = emotionCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("NEUTRAL");
            
            json.append("{\"type\":\"EMOTION_PATTERN\",");
            json.append("\"title\":\"主要情绪模式\",");
            json.append("\"content\":\"您在这段时间内主要情绪为").append(translateEmotion(dominantEmotion));
            json.append("，建议关注情绪变化的规律和触发因素。\",");
            json.append("\"confidence\":0.75}");
        }
        
        json.append("],\"growthPlan\":[");
        json.append("\"每天花10分钟进行情绪觉察练习\",");
        json.append("\"记录情绪触发事件，提升自我认知\",");
        json.append("\"保持规律作息，注意身心健康\"");
        json.append("],\"emotionForecast\":{");
        json.append("\"nextWeekRisk\":\"MEDIUM\",");
        json.append("\"triggers\":[\"工作压力\",\"人际关系\"]");
        json.append("}}");
        
        return json.toString();
    }
    
    /**
     * 生成备用洞察列表
     */
    private List<InsightsReportResponse.Insight> generateFallbackInsightsList(List<AnalysisResult> analysisResults) {
        List<InsightsReportResponse.Insight> insights = new ArrayList<>();
        
        Map<String, Long> emotionCounts = analysisResults.stream()
                .filter(ar -> ar.getPrimaryEmotion() != null)
                .collect(Collectors.groupingBy(AnalysisResult::getPrimaryEmotion, Collectors.counting()));
        
        if (!emotionCounts.isEmpty()) {
            String dominantEmotion = emotionCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("NEUTRAL");
            
            InsightsReportResponse.Insight insight = new InsightsReportResponse.Insight();
            insight.setType("EMOTION_PATTERN");
            insight.setTitle("情绪模式观察");
            insight.setContent("您在这段时间内主要情绪为" + translateEmotion(dominantEmotion) + 
                    "，出现了" + emotionCounts.getOrDefault(dominantEmotion, 0L) + "次。" +
                    "建议关注这种情绪出现的规律和触发因素，提升自我觉察能力。");
            insight.setConfidence(0.75);
            insights.add(insight);
        }
        
        return insights;
    }
    
    /**
     * 生成周报总结
     */
    private String generateWeeklySummary(WeeklyReport report) {
        if (report.getDiaryCount() == 0) {
            return "本周没有记录日记，建议养成每天记录的习惯。";
        }
        
        String emotionDesc = report.getMostFrequentEmotion() != null ? 
                translateEmotion(report.getMostFrequentEmotion()) : "平静";
        
        return String.format("本周共记录了%d篇日记，整体情绪状态以%s为主，平均情绪强度%.2f。%s",
                report.getDiaryCount(),
                emotionDesc,
                report.getAverageIntensity() != null ? report.getAverageIntensity() : 0.5,
                getEmotionAdvice(report.getMostFrequentEmotion()));
    }
    
    private String generateWeeklySummaryFromResponse(WeeklyReportResponse report) {
        if (report.getDiaryCount() == 0) {
            return "本周没有记录日记，建议养成每天记录的习惯。";
        }
        
        String emotionDesc = report.getMostFrequentEmotion() != null ? 
                translateEmotion(report.getMostFrequentEmotion()) : "平静";
        
        return String.format("本周共记录了%d篇日记，整体情绪状态以%s为主，平均情绪强度%.2f。%s",
                report.getDiaryCount(),
                emotionDesc,
                report.getAverageIntensity() != null ? report.getAverageIntensity() : 0.5,
                getEmotionAdvice(report.getMostFrequentEmotion()));
    }
    
    private String translateEmotion(String emotion) {
        if (emotion == null) return "平静";
        return switch (emotion) {
            case "HAPPY" -> "快乐";
            case "SAD" -> "悲伤";
            case "ANGRY" -> "愤怒";
            case "FEAR" -> "恐惧";
            case "SURPRISE" -> "惊讶";
            case "DISGUST" -> "厌恶";
            case "ANXIOUS" -> "焦虑";
            default -> "平静";
        };
    }
    
    private String translateDistortion(String type) {
        return switch (type) {
            case "CATASTROPHIZING" -> "灾难化思维";
            case "BLACK_WHITE" -> "非黑即白";
            case "OVERGENERALIZATION" -> "过度概括";
            case "MIND_READING" -> "读心术";
            case "EMOTIONAL_REASONING" -> "情绪推理";
            case "SHOULD_STATEMENTS" -> "应该句式";
            case "LABELING" -> "贴标签";
            case "PERSONALIZATION" -> "个人化";
            case "MENTAL_FILTER" -> "心理过滤";
            case "DISQUALIFYING_POSITIVE" -> "否定正面";
            default -> type;
        };
    }
    
    private String getEmotionAdvice(String emotion) {
        if (emotion == null) return "";
        return switch (emotion) {
            case "SAD", "ANXIOUS", "FEAR" -> "建议适当放松，进行一些让自己开心的活动。";
            case "ANGRY" -> "建议练习情绪管理，避免冲动行为。";
            case "HAPPY" -> "继续保持积极的心态！";
            default -> "继续记录，关注自己的情绪变化。";
        };
    }
}
