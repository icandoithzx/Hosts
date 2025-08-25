package com.example.demo.service;

import com.example.demo.dto.HeartbeatRequest;
import com.example.demo.dto.HeartbeatResponse;
import com.example.demo.model.entity.Policy;

public interface HeartbeatService {
    /**
     * 检查客户端策略，并返回相应的响应对象。
     * 如果需要更新，响应对象中将包含最新的策略列表。
     *
     * @param clientId 客户端的唯一标识
     * @param clientPoliciesHash 客户端当前持有的策略聚合哈希
     * @return 一个 {@link HeartbeatResponse} 响应对象
     */
    HeartbeatResponse checkPolicies(String clientId, String clientPoliciesHash);
    
    /**
     * 处理客户端心跳请求（新增）
     * 支持更详细的心跳信息，包括客户端状态更新
     *
     * @param request 心跳请求对象
     * @return 心跳响应对象
     */
    HeartbeatResponse handleHeartbeat(HeartbeatRequest request);

    /**
     * 获取客户端当前最新的生效策略（高并发缓存版本）。
     * 该方法会优先从缓存中获取策略，如果缓存中不存在则从数据库查询。
     *
     * @param clientId 客户端ID
     * @return 客户端当前生效的策略
     */
    Policy getClientEffectivePolicy(String clientId);

    /**
     * 获取客户端策略的缓存哈希值（高并发优化）。
     * 该方法直接从 Redis 缓存获取哈希值，避免重复计算。
     *
     * @param clientId 客户端ID
     * @return 策略哈希值
     */
    String getClientPolicyHash(String clientId);

    /**
     * 预热客户端策略缓存（用于性能优化）。
     * 在策略发生变化时调用，提前计算并缓存结果。
     *
     * @param clientId 客户端ID
     */
    void preWarmClientPolicyCache(String clientId);
}
