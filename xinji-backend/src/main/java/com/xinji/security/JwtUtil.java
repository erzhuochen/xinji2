package com.xinji.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.refresh-threshold}")
    private Long refreshThreshold;
    
    private SecretKey secretKey;
    
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成JWT Token
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 检查Token是否可以刷新(过期前1天内)
     */
    public boolean canRefresh(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }
            Date expiry = claims.getExpiration();
            long timeUntilExpiry = expiry.getTime() - System.currentTimeMillis();
            // 在过期前refreshThreshold毫秒内可以刷新
            return timeUntilExpiry > 0 && timeUntilExpiry <= refreshThreshold;
        } catch (ExpiredJwtException e) {
            // Token已过期但在刷新阈值内，也允许刷新
            long expiredTime = System.currentTimeMillis() - e.getClaims().getExpiration().getTime();
            return expiredTime <= 3600000; // 过期1小时内也可刷新
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 获取Token过期时间(秒)
     */
    public Long getExpirationSeconds() {
        return expiration / 1000;
    }
    
    /**
     * 解析Token
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw e; // 让调用方处理过期情况
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * 从过期Token中获取用户ID(用于刷新)
     */
    public String getUserIdFromExpiredToken(String token) {
        try {
            parseToken(token);
            return null;
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        }
    }
}
