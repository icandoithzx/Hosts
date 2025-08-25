package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.service.impl.DatabaseConnectionPoolMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据库监控控制器
 * 提供数据库连接池状态和性能监控接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/database")
public class DatabaseMonitorController {

    @Autowired
    private DatabaseConnectionPoolMonitorService monitorService;

    /**
     * 获取数据库连接池状态
     */
    @GetMapping("/connection-pool/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionPoolStatus() {
        try {
            Map<String, Object> status = monitorService.getConnectionPoolStatus();
            log.debug("📊 获取连接池状态: {}", status);
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("❌ 获取连接池状态失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.errorWithType(500, "获取连接池状态失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取数据库连接池配置信息
     */
    @GetMapping("/connection-pool/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConnectionPoolConfig() {
        try {
            Map<String, Object> config = monitorService.getConnectionPoolConfiguration();
            log.debug("⚙️ 获取连接池配置: {}", config);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("❌ 获取连接池配置失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.errorWithType(500, "获取连接池配置失败: " + e.getMessage())
            );
        }
    }

    /**
     * 获取连接池性能建议
     */
    @GetMapping("/connection-pool/recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceRecommendations() {
        try {
            Map<String, Object> recommendations = monitorService.getPerformanceRecommendations();
            log.debug("💡 获取性能建议: {}", recommendations);
            return ResponseEntity.ok(ApiResponse.success(recommendations));
        } catch (Exception e) {
            log.error("❌ 获取性能建议失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.errorWithType(500, "获取性能建议失败: " + e.getMessage())
            );
        }
    }

    /**
     * 重置连接池（管理员操作，谨慎使用）
     */
    @PostMapping("/connection-pool/reset")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetConnectionPool() {
        try {
            log.warn("⚠️ 管理员触发连接池重置操作");
            Map<String, Object> result = monitorService.resetConnectionPool();
            
            if ((Boolean) result.getOrDefault("success", false)) {
                log.info("✅ 连接池重置成功: {}", result);
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                log.error("❌ 连接池重置失败: {}", result);
                return ResponseEntity.status(500).body(
                    ApiResponse.errorWithType(500, "连接池重置失败")
                );
            }
        } catch (Exception e) {
            log.error("❌ 连接池重置异常", e);
            return ResponseEntity.status(500).body(
                ApiResponse.errorWithType(500, "连接池重置异常: " + e.getMessage())
            );
        }
    }

    /**
     * 获取数据库健康检查信息
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseHealth() {
        try {
            Map<String, Object> status = monitorService.getConnectionPoolStatus();
            Map<String, Object> health = new HashMap<>();
            health.put("status", status.getOrDefault("healthStatus", "UNKNOWN"));
            health.put("connectionPool", status);
            health.put("timestamp", System.currentTimeMillis());
            
            String healthStatus = (String) status.getOrDefault("healthStatus", "UNKNOWN");
            if ("CRITICAL".equals(healthStatus) || "WARNING".equals(healthStatus)) {
                return ResponseEntity.status(503).body(
                    new ApiResponse<>(503, "数据库连接池状态异常", health)
                );
            } else {
                return ResponseEntity.ok(ApiResponse.success(health));
            }
        } catch (Exception e) {
            log.error("❌ 数据库健康检查失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.errorWithType(500, "数据库健康检查失败: " + e.getMessage())
            );
        }
    }
}