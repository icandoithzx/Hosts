package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.CacheAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存状态监控控制器
 * 用于查看和管理缓存热插拔状态
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheStatusController {

    private final CacheAvailabilityService cacheAvailabilityService;

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
            public final String message = available ? "缓存服务正常运行" : "缓存服务不可用，系统已自动切换到数据库直连模式";
        });
    }

    /**
     * 刷新缓存状态
     */
    @PostMapping("/refresh")
    public ApiResponse<String> refreshCacheStatus() {
        cacheAvailabilityService.refreshCacheStatus();
        String status = cacheAvailabilityService.getCacheStatus();
        return ApiResponse.success("缓存状态已刷新: " + status);
    }
}