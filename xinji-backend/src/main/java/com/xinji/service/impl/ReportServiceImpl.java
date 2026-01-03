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
import com.xinji.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
        
        List<InsightsReportResponse.Insight> insights = new ArrayList<>();
        
        // 情绪模式分析
        if (!analysisResults.isEmpty()) {
            Map<String, Long> emotionCounts = analysisResults.stream()
                    .filter(ar -> ar.getPrimaryEmotion() != null)
                    .collect(Collectors.groupingBy(AnalysisResult::getPrimaryEmotion, Collectors.counting()));
            
            String dominantEmotion = emotionCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("NEUTRAL");
            
            InsightsReportResponse.Insight emotionInsight = new InsightsReportResponse.Insight();
            emotionInsight.setType("EMOTION_PATTERN");
            emotionInsight.setTitle("情绪波动规律");
            emotionInsight.setContent("您在这段时间内主要情绪为" + translateEmotion(dominantEmotion) + 
                    "，出现了" + emotionCounts.getOrDefault(dominantEmotion, 0L) + "次");
            emotionInsight.setConfidence(0.85);
            insights.add(emotionInsight);
            
            // 认知模式分析
            Map<String, Long> distortionCounts = analysisResults.stream()
                    .flatMap(ar -> ar.getCognitiveDistortions() != null ? ar.getCognitiveDistortions().stream() : java.util.stream.Stream.empty())
                    .filter(cd -> cd.getType() != null && !"NONE".equals(cd.getType()))
                    .collect(Collectors.groupingBy(AnalysisResult.CognitiveDistortion::getType, Collectors.counting()));
            
            if (!distortionCounts.isEmpty()) {
                String mainDistortion = distortionCounts.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("");
                
                InsightsReportResponse.Insight cognitiveInsight = new InsightsReportResponse.Insight();
                cognitiveInsight.setType("COGNITIVE_PATTERN");
                cognitiveInsight.setTitle("认知偏差倾向");
                cognitiveInsight.setContent("检测到您有轻微的'" + translateDistortion(mainDistortion) + "'认知偏差倾向，建议关注");
                cognitiveInsight.setConfidence(0.72);
                insights.add(cognitiveInsight);
            }
        }
        
        response.setInsights(insights);
        
        // 成长计划
        List<String> growthPlan = Arrays.asList(
                "建议每天进行10分钟正念冥想",
                "尝试记录3件积极事件",
                "注意压力管理，适时放松"
        );
        response.setGrowthPlan(growthPlan);
        
        // 情绪预测
        InsightsReportResponse.EmotionForecast forecast = new InsightsReportResponse.EmotionForecast();
        forecast.setNextWeekRisk("LOW");
        forecast.setTriggers(Arrays.asList("工作压力", "人际关系"));
        response.setEmotionForecast(forecast);
        
        // 正念练习推荐
        List<InsightsReportResponse.MindfulnessSuggestion> mindfulness = new ArrayList<>();
        InsightsReportResponse.MindfulnessSuggestion breathing = new InsightsReportResponse.MindfulnessSuggestion();
        breathing.setTitle("呼吸冥想");
        breathing.setDuration("10分钟");
        breathing.setUrl("/mindfulness/breathing");
        mindfulness.add(breathing);
        response.setMindfulnessSuggestions(mindfulness);
        
        return response;
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
