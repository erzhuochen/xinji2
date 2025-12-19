package com.xinji.dto.response;

import lombok.Data;

/**
 * 微信支付预下单响应DTO
 */
@Data
public class WechatPrepayResponse {
    
    private String prepayId;
    
    private String appId;
    
    private String timeStamp;
    
    private String nonceStr;
    
    /**
     * 固定格式: prepay_id=xxx
     */
    private String packageValue;
    
    private String signType;
    
    private String paySign;
}
