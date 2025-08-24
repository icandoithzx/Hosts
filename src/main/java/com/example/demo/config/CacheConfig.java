package com.example.demo.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置类
 * 启用Spring Cache注解支持，配置不同缓存区域的TTL
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) throws IOException {
        Map<String, org.redisson.spring.cache.CacheConfig> config = new HashMap<>();
        
        // 策略缓存配置 - 长期缓存，1小时过期
        config.put("policies", new org.redisson.spring.cache.CacheConfig(
                3600000,  // TTL: 1 hour in milliseconds
                1800000   // Max idle time: 30 minutes in milliseconds
        ));
        
        // 客户端策略关联缓存 - 中期缓存，30分钟过期
        config.put("clientPolicies", new org.redisson.spring.cache.CacheConfig(
                1800000,  // TTL: 30 minutes in milliseconds
                900000    // Max idle time: 15 minutes in milliseconds
        ));
        
        // 心跳服务高频缓存 - 短期缓存，5分钟过期（新增）
        config.put("heartbeatCache", new org.redisson.spring.cache.CacheConfig(
                300000,   // TTL: 5 minutes in milliseconds
                150000    // Max idle time: 2.5 minutes in milliseconds
        ));
        
        // 客户端策略哈希缓存 - 超短期高频缓存，1分钟过期（新增）
        config.put("policyHash", new org.redisson.spring.cache.CacheConfig(
                60000,    // TTL: 1 minute in milliseconds
                30000     // Max idle time: 30 seconds in milliseconds
        ));
        
        // 默认缓存配置 - 15分钟过期
        config.put("default", new org.redisson.spring.cache.CacheConfig(
                900000,   // TTL: 15 minutes in milliseconds
                450000    // Max idle time: 7.5 minutes in milliseconds
        ));

        return new RedissonSpringCacheManager(redissonClient, config);
    }
}