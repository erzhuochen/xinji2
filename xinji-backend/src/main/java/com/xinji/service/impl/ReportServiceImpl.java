package com.xinji.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xinji.dto.response.AiWeeklySummary;
import com.xinji.dto.response.InsightsReportResponse;
import com.xinji.dto.response.WeeklyReportResponse;
import com.xinji.entity.Diary;
import com.xinji.entity.User;
import com.xinji.entity.mongo.AnalysisResult;
import com.xinji.entity.mongo.DiaryContent;
import com.xinji.entity.mongo.WeeklyReport;
import com.xinji.exception.BusinessException;
import com.xinji.mapper.DiaryRepository;
import com.xinji.mapper.UserRepository;
import com.xinji.repository.mongo.AnalysisResultRepository;
import com.xinji.repository.mongo.DiaryContentRepository;
import com.xinji.repository.mongo.WeeklyReportRepository;
import com.xinji.service.ReportService;
import com.xinji.util.AESUtil;
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

    @Override
    public void triggerWeeklyReportRefresh(String userId, LocalDate date) {
        if (userId == null || date == null) return;

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String lockKey = WEEKLY_REPORT_REGEN_LOCK_PREFIX + userId + ":" + weekStart;

        // 1分钟防抖：锁存在则不重复触发
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 1, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(ok)) {
            return;
        }

        refreshWeeklyReportAsync(userId, weekStart);
    }

    /**
     * 异步刷新指定周周报（全文版AI总结）
     */
    @org.springframework.scheduling.annotation.Async
    public void refreshWeeklyReportAsync(String userId, LocalDate weekStart) {
        try {
            LocalDate weekEnd = weekStart.plusDays(6);

            // 读取本周日记
            List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(userId, weekStart, weekEnd);
            if (diaries.isEmpty()) {
                // 没有日记则不生成
                return;
            }

            // 读取分析结果（用于关键词/情绪统计）
            List<String> analyzedDiaryIds = diaries.stream()
                    .filter(d -> d.getAnalyzed() == 1)
                    .map(Diary::getId)
                    .toList();
            List<AnalysisResult> analysisResults = analyzedDiaryIds.isEmpty() ?
                    Collections.emptyList() :
                    analysisResultRepository.findByDiaryIdIn(analyzedDiaryIds);

            // 统计
            Map<String, Integer> emotionDistribution = new HashMap<>();
            double totalIntensity = 0;
            for (AnalysisResult ar : analysisResults) {
                if (ar.getPrimaryEmotion() == null) continue;
                emotionDistribution.merge(ar.getPrimaryEmotion(), 1, Integer::sum);
                totalIntensity += ar.getEmotionIntensity() != null ? ar.getEmotionIntensity() : 0;
            }

            String mostFrequentEmotion = emotionDistribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("NEUTRAL");
            Double avgIntensity = analysisResults.isEmpty() ? null : (totalIntensity / analysisResults.size());

            List<String> keywords = analysisResults.stream()
                    .flatMap(ar -> ar.getKeywords() != null ? ar.getKeywords().stream() : java.util.stream.Stream.empty())
                    .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(20)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            keywords = padKeywords(keywords, 20);

            // 全文拼接（做长度控制）
            String diaryText = buildWeeklyDiaryFullText(diaries);

            // 组装 report 用于 prompt
            WeeklyReport tmp = new WeeklyReport();
            tmp.setUserId(userId);
            tmp.setWeekStart(weekStart);
            tmp.setWeekEnd(weekEnd);
            tmp.setDiaryCount(diaries.size());
            tmp.setAnalyzedCount(analysisResults.size());
            tmp.setEmotionDistribution(emotionDistribution);
            tmp.setMostFrequentEmotion(mostFrequentEmotion);
            tmp.setAverageIntensity(avgIntensity);
            tmp.setKeywords(keywords);

            AiWeeklySummary aiSummary = generateAISummaryWithFullText(tmp, diaryText);

            // upsert 保存到Mongo weekly_report
            WeeklyReport report = weeklyReportRepository.findByUserIdAndWeekStart(userId, weekStart)
                    .orElseGet(() -> {
                        WeeklyReport r = new WeeklyReport();
                        r.setId(UUID.randomUUID().toString().replace("-", ""));
                        r.setUserId(userId);
                        r.setWeekStart(weekStart);
                        r.setWeekEnd(weekEnd);
                        return r;
                    });

            report.setDiaryCount(diaries.size());
            report.setAnalyzedCount(analysisResults.size());
            report.setEmotionDistribution(emotionDistribution);
            report.setMostFrequentEmotion(mostFrequentEmotion);
            report.setAverageIntensity(avgIntensity);
            report.setKeywords(keywords);
            report.setSummary(JSON.toJSONString(aiSummary));
            report.setCreatedAt(LocalDateTime.now());

            weeklyReportRepository.save(report);

            // 刷新接口缓存：删除该周缓存，确保立刻生效
            String cacheKey = WEEKLY_REPORT_CACHE_PREFIX + userId + ":" + weekStart;
            redisTemplate.delete(cacheKey);

        } catch (Exception e) {
            log.error("刷新周报失败: userId={}, weekStart={}", userId, weekStart, e);
        }
    }

    /**
     * 拼接本周所有日记正文（解密），并做长度控制，避免prompt过长
     */
    private String buildWeeklyDiaryFullText(List<Diary> diaries) {
        // 控制总长度（字符），避免超token。可按需调整
        final int MAX_TOTAL_CHARS = 6000;
        final int MAX_PER_DIARY_CHARS = 800;

        StringBuilder sb = new StringBuilder();
        int total = 0;

        // 按日期升序拼接
        List<Diary> sorted = diaries.stream()
                .sorted(Comparator.comparing(Diary::getDiaryDate))
                .toList();

        for (Diary d : sorted) {
            Optional<DiaryContent> contentOpt = diaryContentRepository.findById(d.getId());
            if (contentOpt.isEmpty()) continue;

            String plain;
            try {
                plain = aesUtil.decrypt(contentOpt.get().getContent());
            } catch (Exception e) {
                continue;
            }

            if (plain == null) plain = "";
            plain = plain.replaceAll("<[^>]+>", "").trim();
            if (plain.length() > MAX_PER_DIARY_CHARS) {
                plain = plain.substring(0, MAX_PER_DIARY_CHARS) + "...";
            }

            String block = "【" + d.getDiaryDate() + "】\n" + plain + "\n\n";
            if (total + block.length() > MAX_TOTAL_CHARS) {
                break;
            }

            sb.append(block);
            total += block.length();
        }

        return sb.toString();
    }

    /**
     * 全文版：在原有统计信息基础上追加“本周日记全文摘要”喂给AI
     */
    private AiWeeklySummary generateAISummaryWithFullText(WeeklyReport report, String diaryFullText) {
        try {
            String prompt = buildWeeklySummaryPromptWithFullText(report, diaryFullText);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", "你是一个专业的心理咨询师，擅长情绪支持与CBT技巧。请严格按要求输出 JSON。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(DASHSCOPE_API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                String content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                JSONObject obj = JSON.parseObject(content);

                AiWeeklySummary summary = new AiWeeklySummary();
                summary.setSummary(obj.getString("summary"));
                summary.setSuggestions(obj.getList("suggestions", String.class));
                summary.setActionPoints(obj.getList("actionPoints", String.class));
                return summary;
            }
        } catch (Exception e) {
            log.error("调用AI生成全文周报总结失败", e);
        }

        return generateLocalSummary(report);
    }

    private String buildWeeklySummaryPromptWithFullText(WeeklyReport report, String diaryFullText) {
        String keywordsStr = report.getKeywords() == null || report.getKeywords().isEmpty()
                ? "(无)"
                : String.join(", ", report.getKeywords().subList(0, Math.min(5, report.getKeywords().size())));

        return String.format("""
            请根据以下用户一周的日记全文与统计信息，生成一份温暖、专业且可执行的周报总结，并严格按 JSON 返回。

            日期范围: %s 至 %s
            日记总数: %d篇
            分析日记数: %d篇
            主要情绪(统计): %s
            平均情绪强度(统计): %s
            高频关键词(统计): %s

            ===== 本周日记正文（可能已截断） =====
            %s
            ===== 结束 =====

            返回 JSON 结构必须严格符合以下 schema（不要输出多余字段，不要输出 markdown，不要用代码块）：
            {
              "summary": "一段100-180字的总结，语气温暖、具体、有同理心",
              "suggestions": ["建议1", "建议2", "建议3"],
              "actionPoints": ["行动点1", "行动点2"]
            }

            约束：
            - 必须结合日记正文中的具体事件/人物/活动，避免只总结情绪标签
            - suggestions 必须恰好 3 条，每条 18-40 字，具体可操作
            - actionPoints 必须恰好 2 条，每条以动词开头
            - 避免诊断口吻，不要给出医疗结论
            """,
            report.getWeekStart(),
            report.getWeekEnd(),
            report.getDiaryCount(),
            report.getAnalyzedCount(),
            translateEmotion(report.getMostFrequentEmotion()),
            report.getAverageIntensity() == null ? "(无)" : String.format("%.2f", report.getAverageIntensity()),
            keywordsStr,
            diaryFullText == null ? "" : diaryFullText
        );
    }

    
    private final DiaryRepository diaryRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final DiaryContentRepository diaryContentRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AESUtil aesUtil;
    
    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;

    @Value("${aliyun.dashscope.model:qwen-plus}")
    private String model;

    private static final String WEEKLY_REPORT_CACHE_PREFIX = "report:weekly:";
    private static final String WEEKLY_REPORT_REGEN_LOCK_PREFIX = "report:weekly:regen:";
    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
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

        // 先查 Mongo 已预生成的周报（优先走秒开路径）
        Optional<WeeklyReport> weeklyReportOpt = weeklyReportRepository.findByUserIdAndWeekStart(userId, startDate);

        // 同时计算本周统计（趋势/分布/关键词等仍可实时或从周报读，这里保留实时计算以确保展示最新）
        List<Diary> diaries = diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        List<String> analyzedDiaryIds = diaries.stream()
                .filter(d -> d.getAnalyzed() == 1)
                .map(Diary::getId)
                .collect(Collectors.toList());

        List<AnalysisResult> analysisResults = analyzedDiaryIds.isEmpty() ?
                Collections.emptyList() :
                analysisResultRepository.findByDiaryIdIn(analyzedDiaryIds);

        WeeklyReportResponse response = buildWeeklyReport(startDate, endDate, diaries, analysisResults);

        // 如果 Mongo 里有已生成 summary，则直接用它（避免现场AI）
        if (weeklyReportOpt.isPresent() && weeklyReportOpt.get().getSummary() != null && !weeklyReportOpt.get().getSummary().isBlank()) {
            try {
                response.setSummary(JSON.parseObject(weeklyReportOpt.get().getSummary(), AiWeeklySummary.class));
            } catch (Exception e) {
                log.warn("解析Mongo周报summary失败，将使用实时生成的summary: userId={}, weekStart={}", userId, startDate, e);
            }
        }

        // 仍保留接口缓存（仅缓存统计+summary对象），但 TTL 可以继续 7 天
        String cacheKey = WEEKLY_REPORT_CACHE_PREFIX + userId + ":" + startDate;
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
                        .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                        .entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(20)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                report.setKeywords(padKeywords(allKeywords, 20));
                
                // 生成AI总结并序列化为JSON字符串
                AiWeeklySummary aiSummary = generateWeeklySummary(report);
                report.setSummary(JSON.toJSONString(aiSummary));
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
                
                WeeklyReportResponse.EmotionTrendItem item = new WeeklyReportResponse.EmotionTrendItem();
                item.setDate(diary.getDiaryDate());
                item.setEmotion(ar.getPrimaryEmotion());
                item.setIntensity(ar.getEmotionIntensity());
                emotionTrend.add(item);
                
                emotionDistribution.merge(ar.getPrimaryEmotion(), 1, Integer::sum);
                totalIntensity += ar.getEmotionIntensity();
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
                .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        response.setKeywords(padKeywords(keywords, 20));
        
        // summary 由预生成任务写入Mongo后读取；此处不做现场AI（避免打开/report很慢）
        // 若Mongo没有summary，可在 triggerWeeklyReportRefresh 中异步生成。
        response.setSummary(null);
        
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
                    .filter(cd -> !"NONE".equals(cd.getType()))
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
        breathing.setDuration("5分钟");
        breathing.setUrl("/mindfulness/breathing");
        mindfulness.add(breathing);
        response.setMindfulnessSuggestions(mindfulness);
        
        return response;
    }
    
    /**
     * 调用AI生成周报总结
     */
    private AiWeeklySummary generateAISummary(WeeklyReport report) {
        try {
            // 构建提示词
            String prompt = buildWeeklySummaryPrompt(report);
            
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", "你是一个专业的心理咨询师，擅长情绪支持与CBT技巧。请严格按用户要求输出 JSON。"),
                    Map.of("role", "user", "content", prompt)
            ));
            // 强制 JSON 输出
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
                String content = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                
                // 解析AI返回的JSON
                content = content.replaceAll("```(json)?", "").trim();
                JSONObject obj = JSON.parseObject(content);

                AiWeeklySummary summary = new AiWeeklySummary();
                summary.setSummary(obj.getString("summary"));
                summary.setSuggestions(obj.getList("suggestions", String.class));
                summary.setActionPoints(obj.getList("actionPoints", String.class));

                return summary;
            }
        } catch (Exception e) {
            log.error("调用AI生成周报总结失败", e);
        }
        
        // AI调用失败时返回本地生成的总结
        return generateLocalSummary(report);
    }
    
    /**
     * 构建周报总结提示词
     */
    private String buildWeeklySummaryPrompt(WeeklyReport report) {
        List<String> topKeywords = report.getKeywords() == null ? Collections.emptyList() : report.getKeywords();
        String keywordsStr = topKeywords.isEmpty()
                ? "(无)"
                : String.join(", ", topKeywords.subList(0, Math.min(5, topKeywords.size())));

        return String.format("""
            请根据以下用户一周的日记数据，生成一份温暖、专业且可执行的周报总结，并严格按 JSON 返回。

            日期范围: %s 至 %s
            日记总数: %d篇
            分析日记数: %d篇
            主要情绪: %s (出现%d次)
            平均情绪强度: %.2f
            高频关键词: %s

            返回 JSON 结构必须严格符合以下 schema（不要输出多余字段，不要输出 markdown，不要用代码块）：
            {
              "summary": "一段100-180字的总结，语气温暖、具体、有同理心",
              "suggestions": ["建议1", "建议2", "建议3"],
              "actionPoints": ["行动点1", "行动点2"]
            }

            约束：
            - suggestions 必须恰好 3 条，每条 18-40 字，具体可操作，不要泛泛而谈
            - actionPoints 必须恰好 2 条，每条以动词开头（如：‘安排/记录/练习/尝试/减少/联系’）
            - 避免诊断口吻，不要出现‘你有抑郁/焦虑症’等医疗结论
            """,
            report.getWeekStart(),
            report.getWeekEnd(),
            report.getDiaryCount(),
            report.getAnalyzedCount(),
            translateEmotion(report.getMostFrequentEmotion()),
            report.getEmotionDistribution() == null ? 0 : report.getEmotionDistribution().getOrDefault(report.getMostFrequentEmotion(), 0),
            report.getAverageIntensity() != null ? report.getAverageIntensity() : 0.5,
            keywordsStr
        );
    }
    
    /**
     * 本地生成周报总结（降级方案）
     */
    private AiWeeklySummary generateLocalSummary(WeeklyReport report) {
        AiWeeklySummary summary = new AiWeeklySummary();

        if (report.getDiaryCount() == 0) {
            summary.setSummary("本周没有记录日记，建议养成每天记录的习惯。");
            summary.setSuggestions(Arrays.asList("试着记录今天发生的一件小事。", "不用担心写得好不好，真实感受最重要。", "如果不知从何写起，可以描述一个场景或一种感觉。"));
            summary.setActionPoints(Arrays.asList("写一篇50字以上的日记", "设定一个明天写日记的提醒"));
            return summary;
        }

        String emotionDesc = report.getMostFrequentEmotion() != null ?
                translateEmotion(report.getMostFrequentEmotion()) : "平静";

        String summaryText = String.format("本周共记录了%d篇日记，整体情绪状态以%s为主，平均情绪强度%.2f。",
                report.getDiaryCount(),
                emotionDesc,
                report.getAverageIntensity() != null ? report.getAverageIntensity() : 0.5);

        summary.setSummary(summaryText);
        summary.setSuggestions(Arrays.asList(getEmotionAdvice(report.getMostFrequentEmotion()), "回顾一下本周让你开心的瞬间。", "思考一下，是什么触发了你的主要情绪？"));
        summary.setActionPoints(Arrays.asList("继续保持记录，关注自己的情绪变化", "挑选一个建议，在本周尝试一下"));

        return summary;
    }
    
    private AiWeeklySummary generateWeeklySummary(WeeklyReport report) {
        // 优先使用AI生成总结，失败时降级到本地规则
        try {
            return generateAISummary(report);
        } catch (Exception e) {
            log.error("生成周报总结时出错，使用本地规则", e);
            return generateLocalSummary(report);
        }
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
    
    /**
     * 若关键词不足 targetSize，则循环填充至固定长度
     */
    private List<String> padKeywords(List<String> src, int targetSize) {
        if (src == null) return Collections.emptyList();
        if (src.size() >= targetSize || src.isEmpty()) return src;
        List<String> padded = new ArrayList<>(src);
        int idx = 0;
        while (padded.size() < targetSize) {
            padded.add(src.get(idx % src.size()));
            idx++;
        }
        return padded;
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
