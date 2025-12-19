package com.xinji.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinji.dto.request.DiaryRequest;
import com.xinji.dto.response.DiaryResponse;
import com.xinji.dto.response.PageResponse;
import com.xinji.entity.Diary;
import com.xinji.entity.mongo.DiaryContent;
import com.xinji.exception.BusinessException;
import com.xinji.repository.DiaryRepository;
import com.xinji.repository.mongo.DiaryContentRepository;
import com.xinji.service.DiaryService;
import com.xinji.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 日记服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {
    
    private final DiaryRepository diaryRepository;
    private final DiaryContentRepository diaryContentRepository;
    private final AESUtil aesUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String CREATE_LIMIT_PREFIX = "diary:create:limit:";
    
    @Override
    @Transactional
    public DiaryResponse create(String userId, DiaryRequest request) {
        // 检查创建频率限制(1分钟1条)
        String limitKey = CREATE_LIMIT_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw BusinessException.tooManyRequests("创建过于频繁，请1分钟后重试");
        }
        
        // 创建日记元数据(MySQL)
        Diary diary = new Diary();
        diary.setUserId(userId);
        diary.setTitle(request.getTitle());
        diary.setDiaryDate(request.getDiaryDate() != null ? request.getDiaryDate() : LocalDate.now());
        diary.setIsDraft(Boolean.TRUE.equals(request.getIsDraft()) ? 1 : 0);
        diary.setAnalyzed(0);
        diary.setDeleted(0);
        diaryRepository.insert(diary);
        
        // 创建日记内容(MongoDB)
        DiaryContent content = new DiaryContent();
        content.setId(diary.getId());
        content.setUserId(userId);
        content.setContent(aesUtil.encrypt(request.getContent()));
        content.setPreview(generatePreview(request.getContent()));
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        diaryContentRepository.save(content);
        
        // 设置创建频率限制
        redisTemplate.opsForValue().set(limitKey, "1", 1, TimeUnit.MINUTES);
        
        log.info("日记创建成功: diaryId={}, userId={}, contentLength={}", 
                diary.getId(), userId, request.getContent().length());
        
        return convertToResponse(diary, content, true);
    }
    
    @Override
    public PageResponse<DiaryResponse> list(String userId, Integer page, Integer pageSize,
                                             LocalDate startDate, LocalDate endDate, String keyword) {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : Math.min(pageSize, 100);
        
        // 查询日记列表
        Page<Diary> pageParam = new Page<>(page, pageSize);
        IPage<Diary> diaryPage = diaryRepository.findByUserIdPage(pageParam, userId, startDate, endDate);
        
        // 获取日记内容预览
        List<String> diaryIds = diaryPage.getRecords().stream()
                .map(Diary::getId)
                .collect(Collectors.toList());
        
        List<DiaryContent> contents = diaryContentRepository.findAllById(diaryIds);
        
        // 转换响应
        List<DiaryResponse> list = diaryPage.getRecords().stream()
                .map(diary -> {
                    DiaryContent content = contents.stream()
                            .filter(c -> c.getId().equals(diary.getId()))
                            .findFirst()
                            .orElse(null);
                    return convertToResponse(diary, content, false);
                })
                .collect(Collectors.toList());
        
        // 关键词过滤(在内存中过滤预览内容)
        if (keyword != null && !keyword.isEmpty()) {
            list = list.stream()
                    .filter(d -> d.getPreview() != null && d.getPreview().contains(keyword))
                    .collect(Collectors.toList());
        }
        
        return new PageResponse<>(diaryPage.getTotal(), page, pageSize, list);
    }
    
    @Override
    public DiaryResponse getById(String userId, String diaryId) {
        Diary diary = diaryRepository.selectById(diaryId);
        
        if (diary == null || diary.getDeleted() == 1) {
            throw BusinessException.notFound("日记不存在");
        }
        
        if (!diary.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权访问该日记");
        }
        
        DiaryContent content = diaryContentRepository.findById(diaryId).orElse(null);
        
        return convertToResponse(diary, content, true);
    }
    
    @Override
    @Transactional
    public DiaryResponse update(String userId, String diaryId, DiaryRequest request) {
        Diary diary = diaryRepository.selectById(diaryId);
        
        if (diary == null || diary.getDeleted() == 1) {
            throw BusinessException.notFound("日记不存在");
        }
        
        if (!diary.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权修改该日记");
        }
        
        // 更新日记元数据
        if (request.getTitle() != null) {
            diary.setTitle(request.getTitle());
        }
        if (request.getIsDraft() != null) {
            diary.setIsDraft(request.getIsDraft() ? 1 : 0);
        }
        diaryRepository.updateById(diary);
        
        // 更新日记内容
        if (request.getContent() != null) {
            DiaryContent content = diaryContentRepository.findById(diaryId).orElse(new DiaryContent());
            content.setId(diaryId);
            content.setUserId(userId);
            content.setContent(aesUtil.encrypt(request.getContent()));
            content.setPreview(generatePreview(request.getContent()));
            content.setUpdatedAt(LocalDateTime.now());
            diaryContentRepository.save(content);
            
            // 内容变化，标记为未分析
            diary.setAnalyzed(0);
            diary.setAnalysisId(null);
            diary.setPrimaryEmotion(null);
            diary.setEmotionIntensity(null);
            diaryRepository.updateById(diary);
        }
        
        log.info("日记更新成功: diaryId={}, userId={}", diaryId, userId);
        
        DiaryContent content = diaryContentRepository.findById(diaryId).orElse(null);
        return convertToResponse(diary, content, true);
    }
    
    @Override
    @Transactional
    public void delete(String userId, String diaryId) {
        Diary diary = diaryRepository.selectById(diaryId);
        
        if (diary == null) {
            throw BusinessException.notFound("日记不存在");
        }
        
        if (!diary.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权删除该日记");
        }
        
        // 逻辑删除
        diaryRepository.deleteById(diaryId);
        
        log.info("日记删除成功: diaryId={}, userId={}", diaryId, userId);
    }
    
    /**
     * 生成内容预览(前100字)
     */
    private String generatePreview(String content) {
        if (content == null) {
            return "";
        }
        // 去除HTML标签
        String plainText = content.replaceAll("<[^>]+>", "").trim();
        if (plainText.length() <= 100) {
            return plainText;
        }
        return plainText.substring(0, 100) + "...";
    }
    
    /**
     * 转换为响应对象
     */
    private DiaryResponse convertToResponse(Diary diary, DiaryContent content, boolean includeContent) {
        DiaryResponse response = new DiaryResponse();
        response.setId(diary.getId());
        response.setUserId(diary.getUserId());
        response.setTitle(diary.getTitle());
        response.setDiaryDate(diary.getDiaryDate());
        response.setIsDraft(diary.getIsDraft() == 1);
        response.setCreatedAt(diary.getCreateTime());
        response.setUpdatedAt(diary.getUpdateTime());
        response.setAnalyzed(diary.getAnalyzed() == 1);
        response.setAnalysisId(diary.getAnalysisId());
        
        if (content != null) {
            response.setPreview(content.getPreview());
            if (includeContent) {
                response.setContent(aesUtil.decrypt(content.getContent()));
            }
        }
        
        // 情绪信息
        if (diary.getPrimaryEmotion() != null) {
            DiaryResponse.EmotionInfo emotionInfo = new DiaryResponse.EmotionInfo();
            emotionInfo.setPrimary(diary.getPrimaryEmotion());
            emotionInfo.setIntensity(diary.getEmotionIntensity());
            response.setEmotion(emotionInfo);
        }
        
        return response;
    }
}
