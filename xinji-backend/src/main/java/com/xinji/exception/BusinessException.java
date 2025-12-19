package com.xinji.exception;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 参数错误
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
    
    /**
     * 未授权
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }
    
    /**
     * 禁止访问
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }
    
    /**
     * 资源不存在
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
    
    /**
     * 请求过于频繁
     */
    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(429, message);
    }
}
