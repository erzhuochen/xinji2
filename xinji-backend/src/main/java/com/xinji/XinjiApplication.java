package com.xinji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 心迹 - 基于人工智能的个人心理成长服务系统
 * 主启动类
 * 
 * @author Xinji Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class XinjiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(XinjiApplication.class, args);
        System.out.println("===========================================");
        System.out.println("       心迹 - 心理健康服务系统启动成功!");
        System.out.println("===========================================");
    }
}
