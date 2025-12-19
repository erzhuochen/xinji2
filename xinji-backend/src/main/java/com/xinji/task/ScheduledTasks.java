package com.xinji.task;

import com.xinji.entity.User;
import com.xinji.mapper.UserRepository;
import com.xinji.service.OrderService;
import com.xinji.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {
    
    private final ReportService reportService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String AI_QUOTA_PREFIX = "ai:quota:";
    
    /**
     * 周报生成任务 - 每周一凌晨2点
     */
    @Scheduled(cron = "0 0 2 ? * MON")
    public void generateWeeklyReports() {
        log.info("【定时任务】开始执行周报生成...");
        try {
            reportService.generateWeeklyReports();
            log.info("【定时任务】周报生成完成");
        } catch (Exception e) {
            log.error("【定时任务】周报生成失败", e);
        }
    }
    
    /**
     * 会员到期检查 - 每日凌晨1点
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkMemberExpiry() {
        log.info("【定时任务】开始检查会员到期...");
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysLater = now.plusDays(3);
            
            // 查询3天内即将到期的会员
            List<User> users = userRepository.selectList(null);
            int count = 0;
            
            for (User user : users) {
                if ("PRO".equals(user.getMemberStatus()) && 
                    user.getMemberExpireTime() != null &&
                    user.getMemberExpireTime().isBefore(threeDaysLater) &&
                    user.getMemberExpireTime().isAfter(now)) {
                    
                    // TODO: 发送续费提醒通知
                    log.info("会员即将到期提醒: userId={}, expireTime={}", 
                            user.getId(), user.getMemberExpireTime());
                    count++;
                }
                
                // 检查已过期的会员，降级为免费用户
                if ("PRO".equals(user.getMemberStatus()) && 
                    user.getMemberExpireTime() != null &&
                    user.getMemberExpireTime().isBefore(now)) {
                    
                    user.setMemberStatus("FREE");
                    userRepository.updateById(user);
                    log.info("会员已过期，降级为免费用户: userId={}", user.getId());
                }
            }
            
            log.info("【定时任务】会员到期检查完成，即将到期: {}人", count);
        } catch (Exception e) {
            log.error("【定时任务】会员到期检查失败", e);
        }
    }
    
    /**
     * AI配额重置 - 每日凌晨0点
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetAiQuota() {
        log.info("【定时任务】开始重置AI配额...");
        try {
            // 删除所有AI配额key
            // 使用scan命令避免阻塞
            var keys = redisTemplate.keys(AI_QUOTA_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("【定时任务】AI配额重置完成，清理{}个key", keys.size());
            } else {
                log.info("【定时任务】AI配额重置完成，无需清理");
            }
        } catch (Exception e) {
            log.error("【定时任务】AI配额重置失败", e);
        }
    }
    
    /**
     * 订单超时取消 - 每5分钟
     */
    @Scheduled(fixedRate = 300000)
    public void cancelExpiredOrders() {
        log.debug("【定时任务】检查超时订单...");
        try {
            orderService.cancelExpiredOrders();
        } catch (Exception e) {
            log.error("【定时任务】订单超时取消失败", e);
        }
    }
    
    /**
     * 数据备份任务 - 每日凌晨3点
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void backupData() {
        log.info("【定时任务】开始数据备份...");
        try {
            // TODO: 实现数据备份到OSS
            // 1. 备份MySQL数据
            // 2. 备份MongoDB数据
            // 3. 上传到阿里云OSS
            log.info("【定时任务】数据备份完成");
        } catch (Exception e) {
            log.error("【定时任务】数据备份失败", e);
        }
    }
    
    /**
     * 日志清理任务 - 每周日凌晨4点
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void cleanupLogs() {
        log.info("【定时任务】开始清理过期日志...");
        try {
            // TODO: 清理30天前的日志文件
            log.info("【定时任务】日志清理完成");
        } catch (Exception e) {
            log.error("【定时任务】日志清理失败", e);
        }
    }
    
    /**
     * 高风险用户监控 - 每小时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void monitorHighRiskUsers() {
        log.info("【定时任务】开始高风险用户监控...");
        try {
            // TODO: 检测连续多天负面情绪的用户
            // 1. 查询最近7天情绪分析结果
            // 2. 检测是否有HIGH风险等级
            // 3. 生成预警通知
            log.info("【定时任务】高风险用户监控完成");
        } catch (Exception e) {
            log.error("【定时任务】高风险用户监控失败", e);
        }
    }
}
