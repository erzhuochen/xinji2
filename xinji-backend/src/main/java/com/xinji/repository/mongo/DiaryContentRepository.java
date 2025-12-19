package com.xinji.repository.mongo;

import com.xinji.entity.mongo.DiaryContent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 日记内容MongoDB数据访问层
 */
@Repository
public interface DiaryContentRepository extends MongoRepository<DiaryContent, String> {
}
