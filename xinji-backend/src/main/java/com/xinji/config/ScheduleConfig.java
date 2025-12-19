package com.xinji.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务配置
 */
@Configuration
@EnableScheduling
public class ScheduleConfig {
    
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("xinji-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            // 任务被拒绝时的处理策略：由调用线程执行
            if (!executor.isShutdown()) {
                r.run();
            }
        });
        return scheduler;
    }
}
