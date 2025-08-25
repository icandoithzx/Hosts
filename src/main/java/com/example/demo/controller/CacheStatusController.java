package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.CacheAvailabilityService;
import com.example.demo.service.DynamicCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 缓存状态监控控制器
 * 用于查看和管理动态缓存状态
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheStatusController {

    private final CacheAvailabilityService cacheAvailabilityService;
    private final DynamicCacheService dynamicCacheService;

    /**
     * 获取缓存状态
     */
    @GetMapping("/status")
    public ApiResponse<Object> getCacheStatus() {
        boolean available = cacheAvailabilityService.isCacheAvailable();
        String status = cacheAvailabilityService.getCacheStatus();
        
        return ApiResponse.success(new Object() {
            public final boolean available = cacheAvailabilityService.isCacheAvailable();
            public final String status = cacheAvailabilityService.getCacheStatus();
            public final String currentMode = dynamicCacheService.getCurrentMode();
            public final boolean dynamicCacheAvailable = dynamicCacheService.isAvailable();
            public final String message = available ? "缓存服务正常运行 - " + dynamicCacheService.getCurrentMode() : "缓存服务不可用";
        });
    }

    /**
     * 刷新缓存状态
     */
    @PostMapping("/refresh")
    public ApiResponse<String> refreshCacheStatus() {
        cacheAvailabilityService.refreshCacheStatus();
        String status = cacheAvailabilityService.getCacheStatus();
        String mode = dynamicCacheService.getCurrentMode();
        return ApiResponse.success("缓存状态已刷新: " + status + " (当前模式: " + mode + ")");
    }
    
    /**
     * 测试动态缓存功能
     */
    @PostMapping("/test")
    public ApiResponse<Object> testDynamicCache() {
        try {
            // 测试存储和读取
            String testKey = "test:cache:" + System.currentTimeMillis();
            String testValue = "test_value_" + System.currentTimeMillis();
            
            // 存储测试数据
            dynamicCacheService.putString(testKey, "testField", testValue, 1, java.util.concurrent.TimeUnit.MINUTES);
            
            // 读取测试数据
            String retrievedValue = dynamicCacheService.getString(testKey, "testField");
            
            // 清理测试数据
            dynamicCacheService.evict(testKey);
            
            boolean success = testValue.equals(retrievedValue);
            String mode = dynamicCacheService.getCurrentMode();
            
            return ApiResponse.success(String.format("动态缓存测试%s - 模式: %s, 存储值: %s, 读取值: %s", 
                    success ? "成功" : "失败", mode, testValue, retrievedValue));
                    
        } catch (Exception e) {
            return ApiResponse.error(500, "动态缓存测试失败: " + e.getMessage());
        }
    }
}