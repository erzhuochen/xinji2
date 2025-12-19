-- ============================================
-- 心迹 (XinJi) 数据库初始化脚本
-- MySQL 8.0+
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS xinji DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xinji;

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone_hash` VARCHAR(64) NOT NULL COMMENT '手机号哈希值(SHA256)',
    `phone_encrypted` VARCHAR(255) NOT NULL COMMENT '手机号密文(AES加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `member_status` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '会员状态:FREE/PRO',
    `member_expire_time` DATETIME DEFAULT NULL COMMENT '会员到期时间',
    `today_ai_quota` INT NOT NULL DEFAULT 3 COMMENT '今日AI分析额度',
    `used_ai_quota` INT NOT NULL DEFAULT 0 COMMENT '今日已用AI额度',
    `register_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用,1正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_hash` (`phone_hash`),
    KEY `idx_member_status` (`member_status`),
    KEY `idx_member_expire` (`member_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 日记表 (元数据)
-- ============================================
CREATE TABLE IF NOT EXISTS `diary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日记ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content_id` VARCHAR(64) DEFAULT NULL COMMENT 'MongoDB内容文档ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `preview` VARCHAR(200) DEFAULT NULL COMMENT '内容预览',
    `is_draft` TINYINT NOT NULL DEFAULT 0 COMMENT '是否草稿:0否,1是',
    `diary_date` DATE NOT NULL COMMENT '日记日期',
    `analyzed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已分析:0否,1是',
    `analysis_id` VARCHAR(64) DEFAULT NULL COMMENT 'MongoDB分析结果文档ID',
    `primary_emotion` VARCHAR(20) DEFAULT NULL COMMENT '主要情绪',
    `emotion_intensity` DECIMAL(3,2) DEFAULT NULL COMMENT '情绪强度(0-1)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间(软删除)',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_diary_date` (`diary_date`),
    KEY `idx_user_diary_date` (`user_id`, `diary_date`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_content_id` (`content_id`),
    KEY `idx_analysis_id` (`analysis_id`),
    CONSTRAINT `fk_diary_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

-- ============================================
-- 订单表
-- ============================================
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `plan_type` VARCHAR(20) NOT NULL COMMENT '套餐类型:MONTHLY/QUARTERLY/ANNUAL',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态:PENDING/PAID/CANCELLED/REFUNDED/EXPIRED',
    `auto_renew` TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动续费:0否,1是',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式:WECHAT/ALIPAY',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '支付平台交易号',
    `expire_at` DATETIME NOT NULL COMMENT '订单过期时间',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_at` (`expire_at`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================
-- 支付记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `payment_method` VARCHAR(20) NOT NULL COMMENT '支付方式',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '支付平台交易号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态:PENDING/SUCCESS/FAILED',
    `callback_data` TEXT DEFAULT NULL COMMENT '支付回调原始数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_transaction_id` (`transaction_id`),
    CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================
-- 短信验证码表
-- ============================================
CREATE TABLE IF NOT EXISTS `sms_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `phone_hash` VARCHAR(64) NOT NULL COMMENT '手机号哈希值',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` VARCHAR(20) NOT NULL DEFAULT 'LOGIN' COMMENT '类型:LOGIN/BIND/DELETE',
    `used` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用:0否,1是',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone_code` (`phone_hash`, `code`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';

-- ============================================
-- 索引优化说明
-- ============================================
-- 1. user表: phone_hash唯一索引用于登录查询
-- 2. diary表: user_id+diary_date联合索引用于用户日记列表查询
-- 3. order表: user_id+status用于订单列表查询
-- 4. 所有外键都加了索引以优化JOIN查询

-- ============================================
-- MongoDB 集合结构说明 (在MongoDB中执行)
-- ============================================
/*
// 日记内容集合
db.createCollection("diary_contents")
db.diary_contents.createIndex({ "diaryId": 1 }, { unique: true })
db.diary_contents.createIndex({ "userId": 1 })

// 分析结果集合
db.createCollection("analysis_results")
db.analysis_results.createIndex({ "diaryId": 1 }, { unique: true })
db.analysis_results.createIndex({ "userId": 1 })
db.analysis_results.createIndex({ "status": 1 })

// 周报集合
db.createCollection("weekly_reports")
db.weekly_reports.createIndex({ "userId": 1, "weekStart": 1 }, { unique: true })
db.weekly_reports.createIndex({ "createdAt": 1 })
*/
