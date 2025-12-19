package com.xinji.util;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.stereotype.Component;

/**
 * 手机号处理工具类
 */
@Component
public class PhoneUtil {
    
    /**
     * 手机号脱敏
     * 例: 13800138000 -> 138****8000
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 生成手机号哈希(用于数据库查询)
     */
    public static String hash(String phone) {
        return DigestUtil.sha256Hex(phone);
    }
    
    /**
     * 验证手机号格式
     */
    public static boolean isValid(String phone) {
        if (phone == null) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }
}
