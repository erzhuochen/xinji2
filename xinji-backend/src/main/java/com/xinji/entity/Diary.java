package com.xinji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日记元数据实体类 - MySQL
 */
@Data
@TableName("t_diary")
public class Diary {
    
    /**
     * 日记ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 日记标题
     */
    private String title;
    
    /**
     * 日记日期
     */
    private LocalDate diaryDate;
    
    /**
     * 是否草稿: 0-否, 1-是
     */
    private Integer isDraft;
    
    /**
     * 是否已分析: 0-否, 1-是
     */
    private Integer analyzed;
    
    /**
     * 分析ID(关联MongoDB)
     */
    private String analysisId;
    
    /**
     * 主导情绪
     */
    private String primaryEmotion;
    
    /**
     * 情绪强度
     */
    private Double emotionIntensity;
    
    /**
     * 是否删除: 0-否, 1-是
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
