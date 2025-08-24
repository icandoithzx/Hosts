package com.example.demo.service;

import com.example.demo.dto.PolicyDto;
import com.example.demo.model.entity.Policy;

import java.util.List;

public interface PolicyAdminService {

    /**
     * 创建或更新一个策略。
     * @param policyDto 来自API层的策略数据传输对象
     * @return 操作完成后的持久化策略实体
     */
    Policy createOrUpdatePolicy(PolicyDto policyDto);

    /**
     * 根据ID获取策略。
     * @param policyId 策略ID
     * @return 策略实体，如果不存在则返回null
     */
    Policy getPolicyById(Long policyId);

    /**
     * 为一个客户端分配一个策略。
     * @param clientId 客户端ID
     * @param policyId 策略ID (Long类型)
     */
    void assignPolicyToClient(String clientId, Long policyId);

    /**
     * 为多个客户端分配同一个策略。
     * @param clientIdsList 客户端ID列表
     * @param policyId 策略ID (Long类型)
     */
    void assignPolicyToClients(List<String> clientIdsList, Long policyId);

    /**
     * 获取客户端关联的所有策略ID列表。
     * @param clientId 客户端ID
     * @return 策略ID列表
     */
    List<Long> getClientPolicyIds(String clientId);

    /**
     * 创建默认策略。
     * 默认策略对所有客户端生效，且无法修改。
     * @param policyDto 策略数据传输对象
     * @return 创建的默认策略
     */
    Policy createDefaultPolicy(PolicyDto policyDto);

    /**
     * 获取客户端当前生效的策略。
     * 优先级：最新激活的非默认策略 > 默认策略
     * @param clientId 客户端ID
     * @return 生效的策略，如果不存在则返回null
     */
    Policy getEffectivePolicy(String clientId);

    /**
     * 激活客户端的指定策略。
     * 将指定策略设为客户端的当前生效策略。
     * @param clientId 客户端ID
     * @param policyId 要激活的策略ID
     */
    void activatePolicy(String clientId, Long policyId);

    /**
     * 更新策略状态。
     * @param policyId 策略ID
     * @param status 新状态（"enabled" 或 "disabled"）
     */
    void updatePolicyStatus(Long policyId, String status);

    /**
     * 删除一个策略及其所有关联。
     * 此操作会从数据库和缓存中彻底删除策略及其与客户端的全部关联。
     * @param policyId 要删除的策略的ID
     */
    void deletePolicy(Long policyId);
}