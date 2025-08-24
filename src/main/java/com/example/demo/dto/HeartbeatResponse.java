package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // 重要: 序列化时不要包含null或空值的字段
public class HeartbeatResponse {
    /**
     * 是否需要更新策略
     */
    private boolean needsPolicyUpdate;

    /**
     * 服务端计算出的最新策略聚合哈希值
     */
    private String latestPoliciesHash;

    /**
     * 最新的策略列表 (仅在需要更新时填充)
     */
    private List<Map<String, String>> policies;

    /**
     * 当前生效的策略信息（新增）
     */
    private Map<String, Object> effectivePolicy;

    /**
     * 策略更新类型（新增）
     * NEW_POLICY - 新的策略分配
     * POLICY_UPDATED - 策略内容更新
     * POLICY_ACTIVATED - 策略激活状态变化
     * DEFAULT_POLICY - 使用默认策略
     */
    private String updateType;

    /**
     * 服务端时间戳（新增）
     */
    private Long serverTimestamp;

    /**
     * 策略更新消息（新增）
     */
    private String message;

    public HeartbeatResponse(boolean needsPolicyUpdate) {
        this.needsPolicyUpdate = needsPolicyUpdate;
        this.serverTimestamp = System.currentTimeMillis();
    }

    public HeartbeatResponse(boolean needsPolicyUpdate, String latestPoliciesHash, List<Map<String, String>> policies) {
        this.needsPolicyUpdate = needsPolicyUpdate;
        this.latestPoliciesHash = latestPoliciesHash;
        this.policies = policies;
        this.serverTimestamp = System.currentTimeMillis();
    }

    public HeartbeatResponse(boolean needsPolicyUpdate, String updateType, Map<String, Object> effectivePolicy, String message) {
        this.needsPolicyUpdate = needsPolicyUpdate;
        this.updateType = updateType;
        this.effectivePolicy = effectivePolicy;
        this.message = message;
        this.serverTimestamp = System.currentTimeMillis();
    }
}
