# 心迹 (XinJi) 数据库ER图

## 一、MySQL数据库ER图

```mermaid
erDiagram
    t_user ||--o{ t_diary : "一对多"
    t_user ||--o{ t_order : "一对多"
    t_user ||--o{ t_payment_record : "一对多"
    t_user ||--o{ t_ai_quota : "一对多"
    t_order ||--o{ t_payment_record : "一对多"
    
    t_user {
        VARCHAR id PK "用户ID(UUID)"
        VARCHAR phone "手机号(AES加密)"
        VARCHAR phone_hash UK "手机号哈希"
        VARCHAR nickname "昵称"
        VARCHAR avatar "头像URL"
        VARCHAR member_status "会员状态"
        DATETIME member_expire_time "会员到期时间"
        DATETIME register_time "注册时间"
        DATETIME last_login_time "最后登录时间"
        TINYINT deleted "逻辑删除"
        DATETIME create_time "创建时间"
        DATETIME update_time "更新时间"
    }
    
    t_diary {
        VARCHAR id PK "日记ID(UUID)"
        VARCHAR user_id FK "用户ID"
        VARCHAR title "标题"
        DATE diary_date "日记日期"
        TINYINT is_draft "是否草稿"
        TINYINT analyzed "是否已分析"
        VARCHAR analysis_id "分析结果ID(MongoDB)"
        VARCHAR primary_emotion "主要情绪"
        DECIMAL emotion_intensity "情绪强度"
        TINYINT deleted "逻辑删除"
        DATETIME create_time "创建时间"
        DATETIME update_time "更新时间"
    }
    
    t_order {
        VARCHAR id PK "订单ID(UUID)"
        VARCHAR order_no UK "订单号"
        VARCHAR user_id FK "用户ID"
        VARCHAR plan_type "套餐类型"
        DECIMAL amount "订单金额"
        VARCHAR status "订单状态"
        TINYINT auto_renew "是否自动续费"
        VARCHAR payment_method "支付方式"
        VARCHAR transaction_id "交易号"
        DATETIME paid_at "支付时间"
        DATETIME expire_at "订单过期时间"
        TINYINT deleted "逻辑删除"
        DATETIME create_time "创建时间"
        DATETIME update_time "更新时间"
    }
    
    t_payment_record {
        VARCHAR id PK "支付记录ID(UUID)"
        VARCHAR order_id FK "订单ID"
        VARCHAR user_id FK "用户ID"
        VARCHAR payment_method "支付方式"
        DECIMAL amount "支付金额"
        VARCHAR transaction_id "交易号"
        VARCHAR status "支付状态"
        TEXT callback_data "回调数据"
        TINYINT deleted "逻辑删除"
        DATETIME create_time "创建时间"
        DATETIME update_time "更新时间"
    }
    
    t_sms_code {
        BIGINT id PK "ID(自增)"
        VARCHAR phone_hash "手机号哈希"
        VARCHAR code "验证码"
        VARCHAR type "类型"
        TINYINT used "是否已使用"
        DATETIME expire_at "过期时间"
        DATETIME create_time "创建时间"
    }
    
    t_ai_quota {
        BIGINT id PK "ID(自增)"
        VARCHAR user_id FK "用户ID"
        DATE quota_date "配额日期"
        INT total_quota "总配额"
        INT used_quota "已用配额"
        DATETIME create_time "创建时间"
        DATETIME update_time "更新时间"
    }
    
    t_cheer_quote {
        BIGINT id PK "ID(自增)"
        VARCHAR content "加油语句"
        DATETIME create_time "创建时间"
    }
```

## 二、MongoDB集合关系图

```mermaid
erDiagram
    t_diary ||--|| diary_content : "一对一"
    t_diary ||--o| analysis_result : "一对一"
    t_user ||--o{ weekly_report : "一对多"
    
    diary_content {
        String id PK "文档ID(关联t_diary.id)"
        String userId "用户ID"
        String content "日记正文(AES加密)"
        String preview "内容预览"
        DateTime createdAt "创建时间"
        DateTime updatedAt "更新时间"
    }
    
    analysis_result {
        String id PK "分析结果ID"
        String diaryId FK "日记ID(关联t_diary.id)"
        String userId "用户ID"
        String status "分析状态"
        Map emotions "情绪分布"
        String primaryEmotion "主导情绪"
        Double emotionIntensity "情绪强度"
        Array keywords "关键词列表"
        Array cognitiveDistortions "认知偏差"
        Array suggestions "调节建议"
        String riskLevel "风险等级"
        DateTime analyzedAt "分析时间"
        DateTime createdAt "创建时间"
    }
    
    weekly_report {
        String id PK "报告ID"
        String userId FK "用户ID(关联t_user.id)"
        Date weekStart "周开始日期"
        Date weekEnd "周结束日期"
        Integer diaryCount "日记数量"
        Integer analyzedCount "已分析数量"
        Map emotionDistribution "情绪分布"
        String mostFrequentEmotion "最频繁情绪"
        Double averageIntensity "平均情绪强度"
        Array keywords "关键词列表"
        String summary "AI周报总结(JSON)"
        DateTime createdAt "创建时间"
    }
```

## 三、完整数据库架构关系图

```mermaid
erDiagram
    %% MySQL表
    t_user ||--o{ t_diary : "一对多"
    t_user ||--o{ t_order : "一对多"
    t_user ||--o{ t_payment_record : "一对多"
    t_user ||--o{ t_ai_quota : "一对多"
    t_order ||--o{ t_payment_record : "一对多"
    
    %% MongoDB与MySQL关联
    t_diary ||--|| diary_content : "一对一(id关联)"
    t_diary ||--o| analysis_result : "一对一(diaryId关联)"
    t_user ||--o{ weekly_report : "一对多(userId关联)"
    
    t_user {
        VARCHAR id PK
        VARCHAR phone
        VARCHAR phone_hash UK
        VARCHAR member_status
    }
    
    t_diary {
        VARCHAR id PK
        VARCHAR user_id FK
        VARCHAR analysis_id "关联MongoDB"
    }
    
    t_order {
        VARCHAR id PK
        VARCHAR user_id FK
    }
    
    t_payment_record {
        VARCHAR id PK
        VARCHAR order_id FK
        VARCHAR user_id FK
    }
    
    diary_content {
        String id PK "关联t_diary.id"
        String userId
        String content "加密存储"
    }
    
    analysis_result {
        String id PK
        String diaryId "关联t_diary.id"
        String userId
    }
    
    weekly_report {
        String id PK
        String userId "关联t_user.id"
    }
```

## 四、表关系说明

### MySQL表关系

1. **t_user (用户表)** - 核心表
   - 一对多关系：一个用户可以有多个日记、多个订单、多个支付记录、多个配额记录

2. **t_diary (日记表)**
   - 多对一关系：多个日记属于一个用户
   - 关联MongoDB：通过`analysis_id`关联`analysis_result`集合，通过`id`关联`diary_content`集合

3. **t_order (订单表)**
   - 多对一关系：多个订单属于一个用户
   - 一对多关系：一个订单可以有多个支付记录（支持多次支付尝试）

4. **t_payment_record (支付记录表)**
   - 多对一关系：多个支付记录属于一个订单和一个用户

5. **t_ai_quota (AI配额表)**
   - 多对一关系：多个配额记录属于一个用户（按日期区分）

6. **t_sms_code (验证码表)**
   - 通过`phone_hash`与`t_user`关联查询（非外键约束）

7. **t_cheer_quote (心灵加油站表)**
   - 独立表，无外键关系

### MongoDB集合关系

1. **diary_content (日记内容集合)**
   - 与`t_diary`一对一关系，通过`id`字段关联

2. **analysis_result (分析结果集合)**
   - 与`t_diary`一对一关系，通过`diaryId`字段关联

3. **weekly_report (周报集合)**
   - 与`t_user`一对多关系，通过`userId`字段关联

## 五、数据存储策略

- **MySQL**: 存储结构化数据，支持事务和复杂查询
- **MongoDB**: 存储非结构化数据（日记正文、AI分析结果、周报），支持灵活的数据结构
- **Redis**: 缓存热点数据（验证码、配额计数、周报缓存）

## 六、索引设计

### MySQL索引
- `t_user`: `phone_hash`(唯一索引), `member_status`, `member_expire_time`
- `t_diary`: `user_id`, `diary_date`, `(user_id, diary_date)`复合索引
- `t_order`: `order_no`(唯一索引), `user_id`, `status`, `expire_at`
- `t_payment_record`: `order_id`, `user_id`, `transaction_id`

### MongoDB索引
- `diary_content`: `id`(主键), `userId`
- `analysis_result`: `id`(主键), `diaryId`, `userId`
- `weekly_report`: `id`(主键), `userId`, `weekStart`


