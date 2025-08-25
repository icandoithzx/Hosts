package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 日志配置测试类
 * 用于验证不同环境下的日志配置是否正确工作
 */
@Slf4j
@Component
@Profile({"dev", "test", "prod"})
public class LogConfigTest implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // 测试不同级别的日志输出
        log.trace("这是TRACE级别的日志 - 最详细的日志信息");
        log.debug("这是DEBUG级别的日志 - 调试信息");
        log.info("这是INFO级别的日志 - 一般信息");
        log.warn("这是WARN级别的日志 - 警告信息");
        log.error("这是ERROR级别的日志 - 错误信息");
        
        // 测试异常日志
        try {
            throw new RuntimeException("这是一个测试异常");
        } catch (Exception e) {
            log.error("捕获到异常", e);
        }
        
        // 测试带参数的日志
        String userName = "testUser";
        Integer userId = 12345;
        log.info("用户登录成功: 用户名={}, 用户ID={}", userName, userId);
        
        log.info("日志配置测试完成，当前激活的profile: {}", 
                System.getProperty("spring.profiles.active", "default"));
    }
}