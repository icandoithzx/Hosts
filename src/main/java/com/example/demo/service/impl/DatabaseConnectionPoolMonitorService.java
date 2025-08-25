package com.example.demo.service.impl;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库连接池监控服务
 * 负责监控HikariCP连接池的状态和性能指标
 */
@Slf4j
@Service
public class DatabaseConnectionPoolMonitorService {

    @Autowired
    private DataSource dataSource;

    /**
     * 获取连接池状态信息
     */
    public Map<String, Object> getConnectionPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                // 基本连接池信息
                status.put("poolName", hikariDataSource.getPoolName());
                status.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                status.put("minimumIdle", hikariDataSource.getMinimumIdle());
                status.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                status.put("idleTimeout", hikariDataSource.getIdleTimeout());
                status.put("maxLifetime", hikariDataSource.getMaxLifetime());
                
                // 实时状态信息
                status.put("activeConnections", poolMXBean.getActiveConnections());
                status.put("idleConnections", poolMXBean.getIdleConnections());
                status.put("totalConnections", poolMXBean.getTotalConnections());
                status.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                
                // 计算使用率
                double poolUsageRate = (double) poolMXBean.getActiveConnections() / hikariDataSource.getMaximumPoolSize() * 100;
                status.put("poolUsageRate", Math.round(poolUsageRate * 100.0) / 100.0);
                
                // 健康状态评估
                String healthStatus = evaluateHealthStatus(poolMXBean, hikariDataSource);
                status.put("healthStatus", healthStatus);
                
                log.debug("📊 连接池状态: 活跃={}, 空闲={}, 总数={}, 等待={}, 使用率={}%", 
                        poolMXBean.getActiveConnections(),
                        poolMXBean.getIdleConnections(),
                        poolMXBean.getTotalConnections(),
                        poolMXBean.getThreadsAwaitingConnection(),
                        poolUsageRate);
                
            } else {
                status.put("error", "数据源不是HikariDataSource类型");
                log.warn("⚠️ 数据源不是HikariDataSource类型: {}", dataSource.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            log.error("❌ 获取连接池状态失败", e);
            status.put("error", "获取连接池状态失败: " + e.getMessage());
        }
        
        return status;
    }

    /**
     * 定时监控连接池状态（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void monitorConnectionPool() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                int activeConnections = poolMXBean.getActiveConnections();
                int totalConnections = poolMXBean.getTotalConnections();
                int maxPoolSize = hikariDataSource.getMaximumPoolSize();
                int awaitingConnections = poolMXBean.getThreadsAwaitingConnection();
                
                double usageRate = (double) activeConnections / maxPoolSize * 100;
                
                // 正常情况下只记录DEBUG日志
                if (usageRate < 70 && awaitingConnections == 0) {
                    log.debug("📊 连接池监控: 活跃={}/{}, 使用率={}%, 等待={}", 
                            activeConnections, maxPoolSize, 
                            Math.round(usageRate * 100.0) / 100.0, awaitingConnections);
                }
                // 使用率较高时记录INFO日志
                else if (usageRate >= 70 && usageRate < 90) {
                    log.info("📈 连接池使用率较高: 活跃={}/{}, 使用率={}%, 等待={}", 
                            activeConnections, maxPoolSize, 
                            Math.round(usageRate * 100.0) / 100.0, awaitingConnections);
                }
                // 使用率过高或有等待连接时记录WARN日志
                else {
                    log.warn("⚠️ 连接池压力较大: 活跃={}/{}, 使用率={}%, 等待={}", 
                            activeConnections, maxPoolSize, 
                            Math.round(usageRate * 100.0) / 100.0, awaitingConnections);
                    
                    // 如果等待连接数过多，给出优化建议
                    if (awaitingConnections > 5) {
                        log.warn("💡 建议增加连接池大小或优化查询性能");
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ 连接池监控异常", e);
        }
    }

    /**
     * 评估连接池健康状态
     */
    private String evaluateHealthStatus(HikariPoolMXBean poolMXBean, HikariDataSource dataSource) {
        int activeConnections = poolMXBean.getActiveConnections();
        int maxPoolSize = dataSource.getMaximumPoolSize();
        int awaitingConnections = poolMXBean.getThreadsAwaitingConnection();
        
        double usageRate = (double) activeConnections / maxPoolSize * 100;
        
        if (awaitingConnections > 10) {
            return "CRITICAL"; // 严重：大量连接等待
        } else if (usageRate >= 90) {
            return "WARNING"; // 警告：使用率过高
        } else if (usageRate >= 70) {
            return "CAUTION"; // 注意：使用率较高
        } else {
            return "HEALTHY"; // 健康：正常状态
        }
    }

    /**
     * 获取连接池性能建议
     */
    public Map<String, Object> getPerformanceRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                int activeConnections = poolMXBean.getActiveConnections();
                int maxPoolSize = hikariDataSource.getMaximumPoolSize();
                int awaitingConnections = poolMXBean.getThreadsAwaitingConnection();
                double usageRate = (double) activeConnections / maxPoolSize * 100;
                
                // 基于当前状态给出建议
                if (usageRate >= 90) {
                    recommendations.put("poolSize", "建议增加最大连接池大小至 " + (maxPoolSize + 10));
                    recommendations.put("optimization", "考虑优化查询性能或增加连接池大小");
                    recommendations.put("priority", "HIGH");
                } else if (usageRate >= 70) {
                    recommendations.put("monitoring", "建议密切监控连接池使用情况");
                    recommendations.put("optimization", "预备增加连接池大小的方案");
                    recommendations.put("priority", "MEDIUM");
                } else if (usageRate < 30 && maxPoolSize > 20) {
                    recommendations.put("poolSize", "可以考虑适当减少连接池大小以节省资源");
                    recommendations.put("optimization", "当前连接池可能配置过大");
                    recommendations.put("priority", "LOW");
                } else {
                    recommendations.put("status", "连接池配置合理，无需调整");
                    recommendations.put("priority", "NONE");
                }
                
                if (awaitingConnections > 5) {
                    recommendations.put("urgent", "立即检查慢查询和数据库性能");
                    recommendations.put("priority", "URGENT");
                }
                
            } else {
                recommendations.put("error", "数据源类型不支持性能分析");
            }
            
        } catch (Exception e) {
            log.error("❌ 获取性能建议失败", e);
            recommendations.put("error", "获取性能建议失败: " + e.getMessage());
        }
        
        return recommendations;
    }

    /**
     * 重置连接池（紧急情况下使用）
     * 注意：这会断开所有现有连接
     */
    public Map<String, Object> resetConnectionPool() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
                
                // 记录重置前的状态
                int beforeActive = poolMXBean.getActiveConnections();
                int beforeTotal = poolMXBean.getTotalConnections();
                
                // 软重置：关闭空闲连接
                poolMXBean.softEvictConnections();
                
                log.warn("🔄 连接池软重置完成: 重置前活跃连接={}, 总连接={}", beforeActive, beforeTotal);
                
                // 等待一小段时间让连接池稳定
                Thread.sleep(1000);
                
                // 记录重置后的状态
                int afterActive = poolMXBean.getActiveConnections();
                int afterTotal = poolMXBean.getTotalConnections();
                
                result.put("success", true);
                Map<String, Object> beforeResetMap = new HashMap<>();
                beforeResetMap.put("active", beforeActive);
                beforeResetMap.put("total", beforeTotal);
                result.put("beforeReset", beforeResetMap);
                
                Map<String, Object> afterResetMap = new HashMap<>();
                afterResetMap.put("active", afterActive);
                afterResetMap.put("total", afterTotal);
                result.put("afterReset", afterResetMap);
                result.put("message", "连接池软重置完成");
                
            } else {
                result.put("success", false);
                result.put("error", "数据源不支持连接池重置");
            }
            
        } catch (Exception e) {
            log.error("❌ 连接池重置失败", e);
            result.put("success", false);
            result.put("error", "连接池重置失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取连接池配置信息
     */
    public Map<String, Object> getConnectionPoolConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                
                config.put("poolName", hikariDataSource.getPoolName());
                config.put("driverClassName", hikariDataSource.getDriverClassName());
                config.put("jdbcUrl", maskPassword(hikariDataSource.getJdbcUrl()));
                config.put("username", hikariDataSource.getUsername());
                config.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                config.put("minimumIdle", hikariDataSource.getMinimumIdle());
                config.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                config.put("idleTimeout", hikariDataSource.getIdleTimeout());
                config.put("maxLifetime", hikariDataSource.getMaxLifetime());
                config.put("validationTimeout", hikariDataSource.getValidationTimeout());
                config.put("leakDetectionThreshold", hikariDataSource.getLeakDetectionThreshold());
                config.put("connectionTestQuery", hikariDataSource.getConnectionTestQuery());
                
            } else {
                config.put("error", "数据源不是HikariDataSource类型");
            }
            
        } catch (Exception e) {
            log.error("❌ 获取连接池配置失败", e);
            config.put("error", "获取连接池配置失败: " + e.getMessage());
        }
        
        return config;
    }

    /**
     * 隐藏JDBC URL中的敏感信息
     */
    private String maskPassword(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        // 简单的密码隐藏逻辑
        return jdbcUrl.replaceAll("password=[^&;]+", "password=***");
    }
}