-- ============================================
-- 心迹 (XinJi) 数据库初始化脚本
-- MySQL 8.0+
-- 执行方式: mysql -u root -p < init.sql
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS xinji DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xinji;

-- ============================================
-- 用户表
-- ============================================
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id` VARCHAR(36) NOT NULL COMMENT '用户ID(UUID)',
    `phone` VARCHAR(255) NOT NULL COMMENT '手机号(AES加密)',
    `phone_hash` VARCHAR(64) NOT NULL COMMENT '手机号哈希(SHA256,用于查询)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `member_status` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '会员状态:FREE/PRO',
    `member_expire_time` DATETIME DEFAULT NULL COMMENT '会员到期时间',
    `register_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone_hash` (`phone_hash`),
    KEY `idx_member_status` (`member_status`),
    KEY `idx_member_expire` (`member_expire_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 日记表 (元数据, 正文存MongoDB)
-- ============================================
DROP TABLE IF EXISTS `t_diary`;
CREATE TABLE `t_diary` (
    `id` VARCHAR(36) NOT NULL COMMENT '日记ID(UUID)',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `diary_date` DATE NOT NULL COMMENT '日记日期',
    `is_draft` TINYINT NOT NULL DEFAULT 0 COMMENT '是否草稿:0否,1是',
    `analyzed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已分析:0否,1是',
    `analysis_id` VARCHAR(36) DEFAULT NULL COMMENT 'MongoDB分析结果ID',
    `primary_emotion` VARCHAR(20) DEFAULT NULL COMMENT '主要情绪',
    `emotion_intensity` DECIMAL(3,2) DEFAULT NULL COMMENT '情绪强度(0.00-1.00)',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_diary_date` (`diary_date`),
    KEY `idx_user_diary_date` (`user_id`, `diary_date`),
    KEY `idx_user_deleted` (`user_id`, `deleted`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

-- ============================================
-- 订单表
-- ============================================
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
    `id` VARCHAR(36) NOT NULL COMMENT '订单ID(UUID)',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `plan_type` VARCHAR(20) NOT NULL COMMENT '套餐类型:MONTHLY/QUARTERLY/ANNUAL',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态:PENDING/PAID/CANCELLED/REFUNDED/EXPIRED',
    `auto_renew` TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动续费:0否,1是',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式:WECHAT/ALIPAY',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '支付平台交易号',
    `expire_at` DATETIME NOT NULL COMMENT '订单过期时间(30分钟)',
    `paid_at` DATETIME DEFAULT NULL COMMENT '支付时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_at` (`expire_at`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================
-- 支付记录表
-- ============================================
DROP TABLE IF EXISTS `t_payment_record`;
CREATE TABLE `t_payment_record` (
    `id` VARCHAR(36) NOT NULL COMMENT '支付记录ID(UUID)',
    `order_id` VARCHAR(36) NOT NULL COMMENT '订单ID',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `payment_method` VARCHAR(20) NOT NULL COMMENT '支付方式:WECHAT/ALIPAY',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '支付平台交易号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态:PENDING/SUCCESS/FAILED',
    `callback_data` TEXT DEFAULT NULL COMMENT '支付回调原始数据(JSON)',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0否,1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================
-- 短信验证码表
-- ============================================
DROP TABLE IF EXISTS `t_sms_code`;
CREATE TABLE `t_sms_code` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `phone_hash` VARCHAR(64) NOT NULL COMMENT '手机号哈希值',
    `code` VARCHAR(10) NOT NULL COMMENT '验证码',
    `type` VARCHAR(20) NOT NULL DEFAULT 'LOGIN' COMMENT '类型:LOGIN/BIND/DELETE',
    `used` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用:0否,1是',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone_code` (`phone_hash`, `code`),
    KEY `idx_expire_at` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';

-- ============================================
-- AI配额记录表 (每日重置)
-- ============================================
DROP TABLE IF EXISTS `t_ai_quota`;
CREATE TABLE `t_ai_quota` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
    `quota_date` DATE NOT NULL COMMENT '配额日期',
    `total_quota` INT NOT NULL DEFAULT 3 COMMENT '总配额(免费用户3,PRO无限)',
    `used_quota` INT NOT NULL DEFAULT 0 COMMENT '已用配额',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `quota_date`),
    KEY `idx_quota_date` (`quota_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI配额记录表';

-- ============================================
-- 说明
-- ============================================
-- 1. 所有表使用 t_ 前缀
-- 2. 主键使用 UUID (VARCHAR(36))
-- 3. 软删除使用 deleted 字段
-- 4. 时间字段统一使用 create_time, update_time
-- 5. 手机号使用AES加密存储，哈希值用于查询
-- 6. 日记正文存储在MongoDB (diary_contents集合)
-- 7. AI分析结果存储在MongoDB (analysis_results集合)
-- 8. 周报数据存储在MongoDB (weekly_reports集合)

-- ============================================
-- 心灵加油站表
-- ============================================
DROP TABLE IF EXISTS `t_cheer_quote`;
CREATE TABLE `t_cheer_quote` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `content` VARCHAR(500) NOT NULL COMMENT '加油语句',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='心灵加油站表';

-- 插入默认加油语句
INSERT INTO `t_cheer_quote` (`content`) VALUES
('你已经很棒了，坚持下去！'),
('给自己一个微笑，新的一天从此开始。'),
('相信自己，你能做到！'),
('每一次努力都是在为未来铺路。'),
('拥抱当下，珍惜此刻。');

-- ============================================
-- 执行完成提示
-- ============================================
SELECT '心迹数据库初始化完成!' AS message;
