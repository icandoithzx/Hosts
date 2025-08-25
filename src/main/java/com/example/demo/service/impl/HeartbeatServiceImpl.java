package com.example.demo.service.impl;

import com.example.demo.dto.HeartbeatRequest;
import com.example.demo.dto.HeartbeatResponse;
import com.example.demo.model.entity.Policy;
import com.example.demo.model.enums.OnlineStatus;
import com.example.demo.service.CacheAvailabilityService;
import com.example.demo.service.DynamicCacheService;
import com.example.demo.service.HeartbeatService;
import com.example.demo.service.HostService;
import com.example.demo.service.PolicyAdminService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 心跳服务实现类
 * 提供高并发、多层缓存的心跳检查和策略管理功能
 */
@Slf4j
@Service
public class HeartbeatServiceImpl implements HeartbeatService {

    @Autowired
    private PolicyAdminService policyAdminService;
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private CacheAvailabilityService cacheAvailabilityService;
    
    @Autowired
    private DynamicCacheService dynamicCacheService;
    
    @Autowired
    private HostService hostService;
    
    // 异步执行器，用于在线状态更新
    private final Executor onlineStatusUpdateExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "heartbeat-online-status-updater");
                t.setDaemon(true); // 设置为守护线程
                return t;
            }
    );

    // 缓存配置常量
    private static final String POLICY_CACHE_PREFIX = "heartbeat:policy:";
    private static final String POLICY_HASH_CACHE_PREFIX = "heartbeat:hash:";
    private static final int CACHE_TTL_MINUTES = 30;

    @Override
    public HeartbeatResponse checkPolicies(String clientId, String clientPoliciesHash) {
        // 参数验证
        if (!StringUtils.hasText(clientId)) {
            return new HeartbeatResponse(false, "INVALID_CLIENT", null, "客户端ID不能为空");
        }

        try {
            // “心跳”逻辑：更新客户端在线状态
            updateClientOnlineStatus(clientId);
            
            // 获取客户端当前生效的策略
            Policy effectivePolicy = getClientEffectivePolicy(clientId);
            
            if (effectivePolicy == null) {
                // 没有生效策略，返回默认处理
                return new HeartbeatResponse(false, "NO_POLICY", null, "客户端暂无分配策略");
            }

            // 计算当前策略的哈希值
            String currentPolicyHash = calculatePolicyHash(effectivePolicy);
            
            // 比较哈希值，判断是否需要更新
            boolean needsUpdate = !currentPolicyHash.equals(clientPoliciesHash);
            
            if (needsUpdate) {
                // 需要更新，构建完整响应
                Map<String, Object> policyData = convertPolicyToMap(effectivePolicy);
                String updateType = determineUpdateType(clientPoliciesHash);
                
                HeartbeatResponse response = new HeartbeatResponse(true, updateType, policyData, "策略需要更新");
                response.setLatestPoliciesHash(currentPolicyHash);
                
                log.info("🔄 客户端 {} 策略需要更新，类型: {}", clientId, updateType);
                return response;
            } else {
                // 不需要更新
                log.debug("✅ 客户端 {} 策略已是最新", clientId);
                return new HeartbeatResponse(false, "UP_TO_DATE", null, "策略已是最新");
            }
            
        } catch (Exception e) {
            // 异常处理
            log.error("⚠️ 客户端 {} 心跳检查失败", clientId, e);
            return new HeartbeatResponse(false, "ERROR", null, "检查策略时发生错误: " + e.getMessage());
        }
    }

    @Override
    public HeartbeatResponse handleHeartbeat(HeartbeatRequest request) {
        // 参数验证
        if (request == null || !StringUtils.hasText(request.getClientId())) {
            return new HeartbeatResponse(false, "INVALID_REQUEST", null, "无效的心跳请求");
        }

        try {
            String clientId = request.getClientId();
            log.info("💓 收到客户端 {} 的心跳信号，版本: {}", 
                    clientId, request.getClientVersion());
            
            // “心跳”逻辑：更新客户端在线状态
            updateClientOnlineStatus(clientId);
            
            // 检查策略是否需要更新
            HeartbeatResponse response = checkPolicies(
                    clientId, 
                    request.getCurrentPoliciesHash()
            );
            
            // 在响应中添加心跳相关信息
            response.setServerTimestamp(System.currentTimeMillis());
            
            return response;
            
        } catch (Exception e) {
            log.error("⚠️ 处理客户端 {} 心跳失败", request.getClientId(), e);
            return new HeartbeatResponse(false, "ERROR", null, "心跳处理失败: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "clientEffectivePolicies", key = "#clientId", condition = "@dynamicCacheService.isAvailable()")
    public Policy getClientEffectivePolicy(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return null;
        }

        try {
            // 使用动态缓存管理器获取缓存
            String cacheKey = POLICY_CACHE_PREFIX + clientId;
            Map<String, Object> cachedPolicy = dynamicCacheService.getMap(cacheKey);
            
            if (!cachedPolicy.isEmpty()) {
                Policy policy = reconstructPolicyFromMap(cachedPolicy);
                log.debug("🔍 从{}缓存获取策略: clientId={}, policyId={}", 
                        dynamicCacheService.getCurrentMode(), clientId, policy.getId());
                return policy;
            }

            // 缓存未命中，从数据库获取
            Policy policy = policyAdminService.getEffectivePolicy(clientId);
            
            if (policy != null) {
                // 缓存策略数据
                Map<String, Object> policyData = convertPolicyToMap(policy);
                dynamicCacheService.putMap(cacheKey, policyData, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                
                log.debug("📝 策略已缓存到{}: clientId={}, policyId={}", 
                        dynamicCacheService.getCurrentMode(), clientId, policy.getId());
            }
            
            return policy;
            
        } catch (Exception e) {
            log.warn("⚠️ 缓存操作异常，降级到数据库查询: clientId={}, error={}", clientId, e.getMessage());
            // 缓存异常时降级到数据库查询
            return policyAdminService.getEffectivePolicy(clientId);
        }
    }

    @Override
    public String getClientPolicyHash(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return null;
        }

        try {
            // 使用动态缓存管理器获取哈希值
            String hashCacheKey = POLICY_HASH_CACHE_PREFIX + clientId;
            String cachedHash = dynamicCacheService.getString(hashCacheKey, "hash");
            
            if (StringUtils.hasText(cachedHash)) {
                log.debug("🔍 从{}缓存获取策略哈希: clientId={}, hash={}", 
                        dynamicCacheService.getCurrentMode(), clientId, cachedHash);
                return cachedHash;
            }

            // 缓存未命中，重新计算
            Policy policy = getClientEffectivePolicy(clientId);
            if (policy != null) {
                String hash = calculatePolicyHash(policy);
                
                // 缓存哈希值
                dynamicCacheService.putString(hashCacheKey, "hash", hash, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                
                log.debug("📝 策略哈希已缓存到{}: clientId={}, hash={}", 
                        dynamicCacheService.getCurrentMode(), clientId, hash);
                return hash;
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("⚠️ 缓存哈希操作异常，重新计算: clientId={}, error={}", clientId, e.getMessage());
            // 异常时重新计算
            Policy policy = getClientEffectivePolicy(clientId);
            return policy != null ? calculatePolicyHash(policy) : null;
        }
    }

    @Override
    public void preWarmClientPolicyCache(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return;
        }

        // 检查动态缓存是否可用
        if (!dynamicCacheService.isAvailable()) {
            log.debug("💔 动态缓存不可用，无需预热: clientId={}", clientId);
            return;
        }

        // 异步预热缓存
        CompletableFuture.runAsync(() -> {
            try {
                // 预热策略缓存
                Policy policy = policyAdminService.getEffectivePolicy(clientId);
                if (policy != null) {
                    // 缓存策略数据
                    String cacheKey = POLICY_CACHE_PREFIX + clientId;
                    Map<String, Object> policyData = convertPolicyToMap(policy);
                    dynamicCacheService.putMap(cacheKey, policyData, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                    
                    // 预热哈希缓存
                    String hash = calculatePolicyHash(policy);
                    String hashCacheKey = POLICY_HASH_CACHE_PREFIX + clientId;
                    dynamicCacheService.putString(hashCacheKey, "hash", hash, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                    
                    log.debug("🔥 客户端缓存预热完成({}): clientId={}, policyId={}", 
                            dynamicCacheService.getCurrentMode(), clientId, policy.getId());
                }
                
            } catch (Exception e) {
                // 预热失败不影响主流程，只记录错误
                log.warn("⚠️ 预热客户端缓存失败: clientId={}, error={}", clientId, e.getMessage());
            }
        });
    }

    /**
     * 计算策略的MD5哈希值
     */
    private String calculatePolicyHash(Policy policy) {
        if (policy == null) {
            return "";
        }

        try {
            StringBuilder hashInput = new StringBuilder();
            hashInput.append(policy.getId()).append("|");
            hashInput.append(policy.getName() != null ? policy.getName() : "").append("|");
            hashInput.append(policy.getDescription() != null ? policy.getDescription() : "").append("|");
            hashInput.append(policy.getStatus() != null ? policy.getStatus() : "").append("|");
            hashInput.append(policy.getVersion() != null ? policy.getVersion() : "").append("|");
            hashInput.append(policy.getPriority() != null ? policy.getPriority() : 0).append("|");
            hashInput.append(policy.getUpdatedAt() != null ? policy.getUpdatedAt().toString() : "");

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(hashInput.toString().getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (Exception e) {
            // 哈希计算失败时使用时间戳作为备用
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * 将策略转换为Map格式
     */
    private Map<String, Object> convertPolicyToMap(Policy policy) {
        Map<String, Object> policyMap = new HashMap<>();
        policyMap.put("id", policy.getId());
        policyMap.put("name", policy.getName());
        policyMap.put("description", policy.getDescription());
        policyMap.put("status", policy.getStatus());
        policyMap.put("version", policy.getVersion());
        policyMap.put("priority", policy.getPriority());
        policyMap.put("isDefault", policy.getIsDefault());
        policyMap.put("updatedAt", policy.getUpdatedAt() != null ? policy.getUpdatedAt().toString() : null);
        return policyMap;
    }

    /**
     * 判断更新类型
     */
    private String determineUpdateType(String clientHash) {
        if (!StringUtils.hasText(clientHash)) {
            return "NEW_POLICY";
        }
        return "POLICY_UPDATED";
    }



    /**
     * 异步更新客户端在线状态
     * 通过客户端ID（通常为MAC地址）查找主机并更新在线状态
     * 使用异步线程池执行，避免阻塞主线程，提高心跳接口响应性能
     * 
     * @param clientId 客户端ID（可能是MAC地址或其他唯一标识）
     */
    private void updateClientOnlineStatus(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return;
        }
        
        // 异步执行在线状态更新，不阻塞心跳主流程
        CompletableFuture.runAsync(() -> {
            try {
                // 尝试通过不同方式查找主机
                
                // 方式1：尝试以clientId作为MAC地址查找
                com.example.demo.model.entity.Host host = hostService.getHostByMacAddress(clientId);
                
                // 方式2：如果上面没找到，尝试以clientId作为主机ID查找
                if (host == null) {
                    try {
                        Long hostId = Long.parseLong(clientId);
                        host = hostService.getHostById(hostId);
                    } catch (NumberFormatException e) {
                        // clientId不是数字，忽略这个尝试
                        log.trace("客户端ID {} 不是数字格式，跳过按ID查找", clientId);
                    }
                }
                
                // 方式3：如果还是没找到，可能需要其他逻辑（比如通过主机名查找）
                // 这里可以根据实际业务需求扩展
                
                if (host != null) {
                    // 找到主机，更新在线状态
                    long startTime = System.currentTimeMillis();
                    hostService.updateOnlineStatus(host.getId(), OnlineStatus.ONLINE);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    log.debug("🟢 客户端 {} 在线状态已更新，主机ID: {}, 主机名: {}, 耗时: {}ms", 
                            clientId, host.getId(), host.getHostName(), duration);
                            
                    // 如果更新耗时过长，记录警告
                    if (duration > 1000) {
                        log.warn("⚠️ 客户端 {} 在线状态更新耗时过长: {}ms", clientId, duration);
                    }
                } else {
                    // 未找到对应主机，记录信息但不影响主流程
                    log.debug("⚠️ 未找到客户端ID {} 对应的主机记录", clientId);
                }
                
            } catch (Exception e) {
                // 更新在线状态失败不影响主流程，只记录错误
                log.warn("⚠️ 异步更新客户端 {} 在线状态失败", clientId, e);
            }
        }, onlineStatusUpdateExecutor)
        .whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("⚠️ 客户端 {} 在线状态异步更新任务执行异常", clientId, throwable);
            }
        });
        
        // 主线程立即返回，不等待异步任务完成
        log.trace("📤 客户端 {} 在线状态更新任务已提交到异步线程池", clientId);
    }

    /**
     * 从Map重构策略对象
     */
    private Policy reconstructPolicyFromMap(Map<String, Object> policyData) {
        try {
            Policy policy = new Policy();
            policy.setId((Long) policyData.get("id"));
            policy.setName((String) policyData.get("name"));
            policy.setDescription((String) policyData.get("description"));
            policy.setStatus((String) policyData.get("status"));
            policy.setVersion((String) policyData.get("version"));
            policy.setPriority((Integer) policyData.get("priority"));
            policy.setIsDefault((Boolean) policyData.get("isDefault"));
            
            String updatedAtStr = (String) policyData.get("updatedAt");
            if (StringUtils.hasText(updatedAtStr)) {
                policy.setUpdatedAt(java.time.LocalDateTime.parse(updatedAtStr));
            }
            
            return policy;
            
        } catch (Exception e) {
            log.warn("⚠️ 从缓存Map重构Policy对象失败: {}", e.getMessage());
            // 重构失败返回null，会触发重新查询
            return null;
        }
    }

    /**
     * 从缓存重构策略对象
     */
    private Policy reconstructPolicyFromCache(RMap<String, Object> cachedPolicy) {
        try {
            Policy policy = new Policy();
            policy.setId((Long) cachedPolicy.get("id"));
            policy.setName((String) cachedPolicy.get("name"));
            policy.setDescription((String) cachedPolicy.get("description"));
            policy.setStatus((String) cachedPolicy.get("status"));
            policy.setVersion((String) cachedPolicy.get("version"));
            policy.setPriority((Integer) cachedPolicy.get("priority"));
            policy.setIsDefault((Boolean) cachedPolicy.get("isDefault"));
            
            String updatedAtStr = (String) cachedPolicy.get("updatedAt");
            if (StringUtils.hasText(updatedAtStr)) {
                policy.setUpdatedAt(java.time.LocalDateTime.parse(updatedAtStr));
            }
            
            return policy;
            
        } catch (Exception e) {
            // 重构失败返回null，会触发重新查询
            return null;
        }
    }
}