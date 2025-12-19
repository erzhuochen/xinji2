package com.xinji.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.xinji.dto.request.DeleteAccountRequest;
import com.xinji.dto.request.LoginRequest;
import com.xinji.dto.request.SendCodeRequest;
import com.xinji.dto.request.UpdateProfileRequest;
import com.xinji.dto.response.LoginResponse;
import com.xinji.dto.response.UserProfileResponse;
import com.xinji.entity.User;
import com.xinji.exception.BusinessException;
import com.xinji.repository.UserRepository;
import com.xinji.security.JwtUtil;
import com.xinji.service.UserService;
import com.xinji.util.AESUtil;
import com.xinji.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final AESUtil aesUtil;
    
    @Value("${sms.mock-enabled:true}")
    private boolean smsMockEnabled;
    
    @Value("${sms.expire-minutes:5}")
    private int codeExpireMinutes;
    
    @Value("${sms.daily-limit:10}")
    private int dailyLimit;
    
    @Value("${sms.hourly-limit:5}")
    private int hourlyLimit;
    
    @Value("${ai-quota.free-daily-limit:5}")
    private int freeDailyLimit;
    
    @Value("${ai-quota.pro-daily-limit:1000}")
    private int proDailyLimit;
    
    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String CODE_COUNT_HOUR_PREFIX = "sms:count:hour:";
    private static final String CODE_COUNT_DAY_PREFIX = "sms:count:day:";
    private static final String AI_QUOTA_PREFIX = "ai:quota:";
    
    @Override
    public void sendCode(SendCodeRequest request) {
        String phone = request.getPhone();
        
        // 检查发送频率限制
        checkSendLimit(phone);
        
        // 生成6位验证码
        String code = RandomUtil.randomNumbers(6);
        
        if (smsMockEnabled) {
            // 开发阶段模拟发送
            log.info("【心迹】模拟验证码: {} -> {}", phone, code);
        } else {
            // TODO: 调用真实短信服务
            log.info("发送验证码到: {}", PhoneUtil.mask(phone));
        }
        
        // 存储验证码到Redis
        String codeKey = CODE_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, code, codeExpireMinutes, TimeUnit.MINUTES);
        
        // 增加发送次数
        incrementSendCount(phone);
    }
    
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String phone = request.getPhone();
        String code = request.getCode();
        
        // 验证验证码
        String codeKey = CODE_KEY_PREFIX + phone;
        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        
        if (storedCode == null || !code.equals(storedCode.toString())) {
            throw BusinessException.badRequest("验证码错误或已过期");
        }
        
        // 验证成功，删除验证码
        redisTemplate.delete(codeKey);
        
        // 查询或创建用户
        String phoneHash = PhoneUtil.hash(phone);
        User user = userRepository.findByPhoneHash(phoneHash);
        
        boolean isNewUser = false;
        if (user == null) {
            // 新用户注册
            user = new User();
            user.setPhone(aesUtil.encrypt(phone));
            user.setPhoneHash(phoneHash);
            user.setMemberStatus("FREE");
            user.setDeleted(0);
            userRepository.insert(user);
            isNewUser = true;
            log.info("新用户注册: {}", PhoneUtil.mask(phone));
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userRepository.updateById(user);
        
        // 生成JWT Token
        String token = jwtUtil.generateToken(user.getId());
        
        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(jwtUtil.getExpirationSeconds());
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setPhone(PhoneUtil.mask(phone));
        userInfo.setMemberStatus(user.getMemberStatus());
        userInfo.setMemberExpireTime(user.getMemberExpireTime());
        userInfo.setRegisterTime(user.getRegisterTime());
        response.setUser(userInfo);
        
        log.info("用户登录成功: userId={}, isNewUser={}", user.getId(), isNewUser);
        return response;
    }
    
    @Override
    public LoginResponse refreshToken(String oldToken) {
        if (!jwtUtil.canRefresh(oldToken)) {
            throw BusinessException.unauthorized("Token无法刷新，请重新登录");
        }
        
        String userId = jwtUtil.getUserIdFromToken(oldToken);
        if (userId == null) {
            userId = jwtUtil.getUserIdFromExpiredToken(oldToken);
        }
        
        if (userId == null) {
            throw BusinessException.unauthorized("无效的Token");
        }
        
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("用户不存在");
        }
        
        // 生成新Token
        String newToken = jwtUtil.generateToken(userId);
        
        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setExpiresIn(jwtUtil.getExpirationSeconds());
        
        log.info("Token刷新成功: userId={}", userId);
        return response;
    }
    
    @Override
    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setPhone(PhoneUtil.mask(aesUtil.decrypt(user.getPhone())));
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setMemberStatus(user.getMemberStatus());
        response.setMemberExpireTime(user.getMemberExpireTime());
        response.setRegisterTime(user.getRegisterTime());
        
        // 统计日记数量
        Integer diaryCount = userRepository.countDiaries(userId);
        response.setDiaryCount(diaryCount);
        
        // 获取AI配额信息
        boolean isPro = "PRO".equals(user.getMemberStatus());
        response.setTodayAiQuota(isPro ? proDailyLimit : freeDailyLimit);
        response.setUsedAiQuota(getUsedAiQuota(userId));
        
        return response;
    }
    
    @Override
    @Transactional
    public void updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        
        userRepository.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);
    }
    
    @Override
    public String exportData(String userId) {
        // TODO: 实现异步数据导出，生成下载链接
        log.info("用户数据导出请求: userId={}", userId);
        return "https://oss.xiniji.com/export/" + userId + "_" + System.currentTimeMillis() + ".zip";
    }
    
    @Override
    @Transactional
    public void deleteAccount(String userId, DeleteAccountRequest request) {
        // 验证确认文字
        if (!"我确认删除账号".equals(request.getConfirmText())) {
            throw BusinessException.badRequest("确认文字不正确");
        }
        
        // 验证验证码
        String codeKey = CODE_KEY_PREFIX + request.getPhone();
        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        
        if (storedCode == null || !request.getCode().equals(storedCode.toString())) {
            throw BusinessException.badRequest("验证码错误或已过期");
        }
        
        // 验证手机号是否匹配
        User user = userRepository.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        
        String phoneHash = PhoneUtil.hash(request.getPhone());
        if (!phoneHash.equals(user.getPhoneHash())) {
            throw BusinessException.badRequest("手机号与账号不匹配");
        }
        
        // 逻辑删除用户
        userRepository.deleteById(userId);
        
        // 删除验证码
        redisTemplate.delete(codeKey);
        
        log.info("用户账号已注销: userId={}", userId);
    }
    
    /**
     * 检查发送频率限制
     */
    private void checkSendLimit(String phone) {
        String hourKey = CODE_COUNT_HOUR_PREFIX + phone;
        String dayKey = CODE_COUNT_DAY_PREFIX + phone;
        
        // 检查小时限制
        Object hourCount = redisTemplate.opsForValue().get(hourKey);
        if (hourCount != null && Integer.parseInt(hourCount.toString()) >= hourlyLimit) {
            throw BusinessException.tooManyRequests("请求过于频繁，请1小时后重试");
        }
        
        // 检查天限制
        Object dayCount = redisTemplate.opsForValue().get(dayKey);
        if (dayCount != null && Integer.parseInt(dayCount.toString()) >= dailyLimit) {
            throw BusinessException.tooManyRequests("今日验证码发送次数已达上限");
        }
    }
    
    /**
     * 增加发送次数
     */
    private void incrementSendCount(String phone) {
        String hourKey = CODE_COUNT_HOUR_PREFIX + phone;
        String dayKey = CODE_COUNT_DAY_PREFIX + phone;
        
        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, 1, TimeUnit.HOURS);
        
        redisTemplate.opsForValue().increment(dayKey);
        redisTemplate.expire(dayKey, 1, TimeUnit.DAYS);
    }
    
    /**
     * 获取用户今日已使用AI配额
     */
    private Integer getUsedAiQuota(String userId) {
        String key = AI_QUOTA_PREFIX + userId;
        Object count = redisTemplate.opsForValue().get(key);
        return count == null ? 0 : Integer.parseInt(count.toString());
    }
}
