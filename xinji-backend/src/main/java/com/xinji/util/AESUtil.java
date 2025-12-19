package com.xinji.util;

import cn.hutool.crypto.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * AES加密工具类
 * 用于日记正文等敏感数据加密
 */
@Component
public class AESUtil {
    
    @Value("${aes.key}")
    private String aesKey;
    
    @Value("${aes.iv}")
    private String aesIv;
    
    private AES aes;
    
    @PostConstruct
    public void init() {
        // 确保key和iv为16字节
        byte[] keyBytes = padOrTrim(aesKey.getBytes(StandardCharsets.UTF_8), 16);
        byte[] ivBytes = padOrTrim(aesIv.getBytes(StandardCharsets.UTF_8), 16);
        // 使用 PKCS5Padding (JDK标准支持，与PKCS7Padding在16字节块大小时等效)
        this.aes = new AES("CBC", "PKCS5Padding", keyBytes, ivBytes);
    }
    
    /**
     * 加密
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return aes.encryptBase64(plainText);
    }
    
    /**
     * 解密
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        return aes.decryptStr(encryptedText);
    }
    
    /**
     * 填充或截断到指定长度
     */
    private byte[] padOrTrim(byte[] bytes, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, Math.min(bytes.length, length));
        return result;
    }
}
