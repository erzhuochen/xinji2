package com.xinji.repository.mongo;

import com.xinji.entity.mongo.AnalysisResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI分析结果MongoDB数据访问层
 */
@Repository
public interface AnalysisResultRepository extends MongoRepository<AnalysisResult, String> {
    
    /**
     * 根据日记ID查询分析结果
     */
    Optional<AnalysisResult> findByDiaryId(String diaryId);
    
    /**
     * 查询用户的分析结果列表
     */
    List<AnalysisResult> findByUserIdOrderByAnalyzedAtDesc(String userId);
    
    /**
     * 查询用户指定日记ID列表的分析结果
     */
    List<AnalysisResult> findByDiaryIdIn(List<String> diaryIds);
}
