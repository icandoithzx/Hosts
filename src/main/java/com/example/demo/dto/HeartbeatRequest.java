package com.example.demo.dto;

import lombok.Data;

@Data
public class HeartbeatRequest {
    /**
     * 客户端的唯一标识
     */
    private String clientId;

    /**
     * 客户端当前持有的策略聚合哈希
     */
    private String currentPoliciesHash;

    /**
     * 客户端当前策略版本号（新增）
     */
    private String currentPolicyVersion;

    /**
     * 客户端最后更新策略的时间戳（新增）
     */
    private Long lastPolicyUpdateTime;

    /**
     * 客户端版本信息（可选）
     */
    private String clientVersion;
}