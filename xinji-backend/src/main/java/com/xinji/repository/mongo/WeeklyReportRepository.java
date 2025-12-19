package com.xinji.repository.mongo;

import com.xinji.entity.mongo.WeeklyReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 周报MongoDB数据访问层
 */
@Repository
public interface WeeklyReportRepository extends MongoRepository<WeeklyReport, String> {
    
    /**
     * 查询用户指定周的周报
     */
    Optional<WeeklyReport> findByUserIdAndWeekStart(String userId, LocalDate weekStart);
}
