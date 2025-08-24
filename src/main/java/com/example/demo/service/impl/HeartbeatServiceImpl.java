package com.example.demo.service.impl;

import com.example.demo.dto.HeartbeatResponse;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.HeartbeatService;
import com.example.demo.service.PolicyAdminService;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 心跳服务实现类
 * 提供高并发、多层缓存的心跳检查和策略管理功能
 */
@Service
public class HeartbeatServiceImpl implements HeartbeatService {

    private final PolicyAdminService policyAdminService;
    private final RedissonClient redissonClient;

    // 缓存配置常量
    private static final String POLICY_CACHE_PREFIX = "heartbeat:policy:";
    private static final String POLICY_HASH_CACHE_PREFIX = "heartbeat:hash:";
    private static final int CACHE_TTL_MINUTES = 30;

    public HeartbeatServiceImpl(PolicyAdminService policyAdminService, 
                               RedissonClient redissonClient) {
        this.policyAdminService = policyAdminService;
        this.redissonClient = redissonClient;
    }

    @Override
    public HeartbeatResponse checkPolicies(String clientId, String clientPoliciesHash) {
        // 参数验证
        if (!StringUtils.hasText(clientId)) {
            return new HeartbeatResponse(false, "INVALID_CLIENT", null, "客户端ID不能为空");
        }

        try {
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
                
                return response;
            } else {
                // 不需要更新
                return new HeartbeatResponse(false, "UP_TO_DATE", null, "策略已是最新");
            }
            
        } catch (Exception e) {
            // 异常处理
            return new HeartbeatResponse(false, "ERROR", null, "检查策略时发生错误: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "clientEffectivePolicies", key = "#clientId")
    public Policy getClientEffectivePolicy(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return null;
        }

        try {
            // 先从Redis缓存获取
            String cacheKey = POLICY_CACHE_PREFIX + clientId;
            RMap<String, Object> cachedPolicy = redissonClient.getMap(cacheKey);
            
            if (!cachedPolicy.isEmpty()) {
                return reconstructPolicyFromCache(cachedPolicy);
            }

            // 缓存未命中，从数据库获取
            Policy policy = policyAdminService.getEffectivePolicy(clientId);
            
            if (policy != null) {
                // 缓存到Redis
                cachePolicyToRedis(clientId, policy);
            }
            
            return policy;
            
        } catch (Exception e) {
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
            // 先从Redis哈希缓存获取
            String hashCacheKey = POLICY_HASH_CACHE_PREFIX + clientId;
            RMap<String, String> hashCache = redissonClient.getMap(hashCacheKey);
            String cachedHash = hashCache.get("hash");
            
            if (StringUtils.hasText(cachedHash)) {
                return cachedHash;
            }

            // 缓存未命中，重新计算
            Policy policy = getClientEffectivePolicy(clientId);
            if (policy != null) {
                String hash = calculatePolicyHash(policy);
                
                // 缓存哈希值
                hashCache.put("hash", hash);
                hashCache.expire(CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                
                return hash;
            }
            
            return null;
            
        } catch (Exception e) {
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

        // 异步预热缓存
        CompletableFuture.runAsync(() -> {
            try {
                // 预热策略缓存
                Policy policy = policyAdminService.getEffectivePolicy(clientId);
                if (policy != null) {
                    cachePolicyToRedis(clientId, policy);
                    
                    // 预热哈希缓存
                    String hash = calculatePolicyHash(policy);
                    String hashCacheKey = POLICY_HASH_CACHE_PREFIX + clientId;
                    RMap<String, String> hashCache = redissonClient.getMap(hashCacheKey);
                    hashCache.put("hash", hash);
                    hashCache.expire(CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                }
                
            } catch (Exception e) {
                // 预热失败不影响主流程，只记录错误
                System.err.println("预热客户端缓存失败 [" + clientId + "]: " + e.getMessage());
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
     * 将策略缓存到Redis
     */
    private void cachePolicyToRedis(String clientId, Policy policy) {
        try {
            String cacheKey = POLICY_CACHE_PREFIX + clientId;
            RMap<String, Object> cache = redissonClient.getMap(cacheKey);
            
            cache.put("id", policy.getId());
            cache.put("name", policy.getName());
            cache.put("description", policy.getDescription());
            cache.put("status", policy.getStatus());
            cache.put("version", policy.getVersion());
            cache.put("priority", policy.getPriority());
            cache.put("isDefault", policy.getIsDefault());
            cache.put("updatedAt", policy.getUpdatedAt() != null ? policy.getUpdatedAt().toString() : null);
            
            // 设置过期时间
            cache.expire(CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            
        } catch (Exception e) {
            // 缓存失败不影响主流程
            System.err.println("缓存策略失败: " + e.getMessage());
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