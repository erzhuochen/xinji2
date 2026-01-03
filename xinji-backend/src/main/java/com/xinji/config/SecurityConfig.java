package com.xinji.config;

import com.xinji.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${cors.allowed-methods}")
    private String allowedMethods;
    
    /**
     * 不需要认证的接口路径
     */
    private static final String[] PUBLIC_PATHS = {
            "/auth/send-code",
            "/auth/login",
            "/payment/wechat/notify",
            "/legal/**",
            "/actuator/health",
            "/error"
    };
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF(使用JWT无状态认证)
//            SCRF(Cross-Site Request Forgery，跨站请求伪造)是一种攻击方式，攻击者通过伪造用户请求，诱使用户在不知情的情况下执行非本意的操作。
//            由于本应用使用JWT进行认证，且不依赖于服务器端的Session状态，因此禁用CSRF保护是合理的。
//            这样可以简化请求处理流程，避免不必要的复杂性，同时确保应用的安全性。
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置CORS
            // CORS(Cross-Origin Resource Sharing，跨域资源共享)是一种机制，允许受限资源（如API）在一个域上被另一个域的网页访问。
            // 通过配置CORS，可以控制哪些外部域名可以访问应用的资源，从而增强安全性。
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 无状态Session
            // sessionManagement配置为STATELESS，表示应用不依赖于服务器端的Session来存储用户状态。
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers("/report/insights").hasRole("PRO")
                .requestMatchers("/counselor/**").hasRole("PRO")
                .requestMatchers("/auth/logout", "/auth/refresh-token", "/user/**", "/diary/**", "/analysis/**", "/report/**", "/order/**", "/payment/**", "/cheer-quotes/**").authenticated()
                .anyRequest().denyAll()
            )
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
