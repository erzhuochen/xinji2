package com.xinji.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

/**
 * 日记内容实体类 - MongoDB
 * 存储日记正文(AES加密)
 */
@Data
@Document(collection = "diary_content")
public class DiaryContent {
    
    /**
     * 文档ID(与MySQL日记ID一致)
     */
    @Id
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 日记正文(AES加密)
     */
    private String content;
    
    /**
     * 内容预览(前100字,明文)
     */
    private String preview;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
