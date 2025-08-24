package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.dto.PolicyDto;
import com.example.demo.mapper.ClientPolicyMappingMapper;
import com.example.demo.mapper.PolicyMapper;
import com.example.demo.model.entity.ClientPolicyMapping;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.HeartbeatService;
import com.example.demo.service.PolicyAdminService;
import com.example.demo.util.SnowflakeIdGenerator;
import org.redisson.api.RBatch;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import com.example.demo.service.CacheAvailabilityService;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class PolicyAdminServiceImpl implements PolicyAdminService {

    @Autowired
    private PolicyMapper policyMapper;
    
    @Autowired
    private ClientPolicyMappingMapper clientPolicyMappingMapper;
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    @Autowired
    private HeartbeatService heartbeatService;
    
    @Autowired
    private CacheAvailabilityService cacheAvailabilityService;


    @Override
    @Transactional
    @CachePut(value = "policies", key = "#result.id", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Policy createOrUpdatePolicy(PolicyDto policyDto) {
        if (policyDto == null) {
            throw new IllegalArgumentException("策略数据不能为null");
        }

        Policy policy = convertDtoToEntity(policyDto);
        policy.setVersion(String.valueOf(System.currentTimeMillis()));

        // 检查是否为更新操作且目标是默认策略
        boolean isUpdate = policy.getId() != null;
        if (isUpdate) {
            Policy existingPolicy = policyMapper.selectById(policy.getId());
            if (existingPolicy != null && Boolean.TRUE.equals(existingPolicy.getIsDefault())) {
                throw new IllegalArgumentException("默认策略不可修改");
            }
            policy.setUpdatedAt(LocalDateTime.now());
            policyMapper.updateById(policy);
        } else {
            // 新创建策略
            policy.setId(snowflakeIdGenerator.nextId());
            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            policyMapper.insert(policy);
        }

        // 策略创建/更新后，异步预热相关客户端缓存
        preWarmAffectedClients(policy.getId(), isUpdate);

        return policy;
    }

    @Override
    @Cacheable(value = "policies", key = "#policyId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Policy getPolicyById(Long policyId) {
        if (policyId == null) {
            return null;
        }
        return policyMapper.selectById(policyId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "clientPolicies", key = "#clientId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void assignPolicyToClient(String clientId, Long policyId) {
        if (clientId == null || policyId == null) {
            return;
        }

        // 1. 检查策略是否存在
        Policy policy = policyMapper.selectById(policyId);
        if (policy == null) {
            throw new IllegalArgumentException("策略不存在: " + policyId);
        }

        // 2. 检查是否已存在关联
        QueryWrapper<ClientPolicyMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("client_id", clientId).eq("policy_id", policyId);
        ClientPolicyMapping existingMapping = clientPolicyMappingMapper.selectOne(queryWrapper);
        
        LocalDateTime now = LocalDateTime.now();
        
        if (existingMapping != null) {
            // 已存在关联，更新激活状态
            deactivateAllClientPolicies(clientId);
            existingMapping.setIsActive(true);
            existingMapping.setActivatedAt(now);
            clientPolicyMappingMapper.updateById(existingMapping);
        } else {
            // 不存在关联，创建新的
            deactivateAllClientPolicies(clientId);
            
            ClientPolicyMapping mapping = new ClientPolicyMapping();
            mapping.setClientId(clientId);
            mapping.setPolicyId(policyId);
            mapping.setAssignedAt(now);
            mapping.setActivatedAt(now);
            mapping.setIsActive(true);
            clientPolicyMappingMapper.insert(mapping);
        }

        // 策略分配后，异步预热客户端缓存
        preWarmClientCache(clientId);
    }

    @Override
    @Transactional
    public void assignPolicyToClients(List<String> clientIdsList, Long policyId) {
        if (clientIdsList == null || policyId == null) {
            return;
        }

        // 1. 验证策略是否存在
        Policy policy = policyMapper.selectById(policyId);
        if (policy == null) {
            throw new IllegalArgumentException("策略不存在: " + policyId);
        }

        // 2. 首先获取该策略当前关联的所有客户端ID（用于后续缓存清理）
        QueryWrapper<ClientPolicyMapping> queryForOldClients = new QueryWrapper<>();
        queryForOldClients.eq("policy_id", policyId).select("client_id");
        List<String> oldClientIds = clientPolicyMappingMapper.selectList(queryForOldClients)
                .stream()
                .map(ClientPolicyMapping::getClientId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 清空该策略关联的所有客户端ID
        QueryWrapper<ClientPolicyMapping> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("policy_id", policyId);
        clientPolicyMappingMapper.delete(deleteWrapper);

        // 4. 全量关联新的客户端ID列表
        LocalDateTime now = LocalDateTime.now();
        for (String clientId : clientIdsList) {
            if (clientId != null && !clientId.trim().isEmpty()) {
                ClientPolicyMapping mapping = new ClientPolicyMapping();
                mapping.setClientId(clientId);
                mapping.setPolicyId(policyId);
                mapping.setAssignedAt(now);
                mapping.setIsActive(false); // 初始状态为非激活
                clientPolicyMappingMapper.insert(mapping);
            }
        }

        // 5. 清理缓存：包括旧客户端和新客户端
        List<String> allAffectedClientIds = new ArrayList<>();
        allAffectedClientIds.addAll(oldClientIds);
        allAffectedClientIds.addAll(clientIdsList);
        // 去重
        allAffectedClientIds = allAffectedClientIds.stream()
                .filter(id -> id != null && !id.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (!allAffectedClientIds.isEmpty()) {
            evictClientPoliciesCache(allAffectedClientIds);
            // 异步预热所有受影响的客户端缓存
            preWarmClientsCaches(allAffectedClientIds);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "policies", key = "#policyId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void deletePolicy(Long policyId) {
        if (policyId == null) {
            return;
        }

        // 1. 在删除数据库前，先找出所有与该策略关联的客户端ID
        QueryWrapper<ClientPolicyMapping> mappingQuery = new QueryWrapper<>();
        mappingQuery.eq("policy_id", policyId).select("client_id");
        List<String> affectedClientIds = clientPolicyMappingMapper.selectList(mappingQuery)
                .stream()
                .map(ClientPolicyMapping::getClientId)
                .collect(Collectors.toList());

        // 2. 在数据库中删除关联和策略本身
        clientPolicyMappingMapper.delete(new QueryWrapper<ClientPolicyMapping>().eq("policy_id", policyId));
        int deletedRows = policyMapper.deleteById(policyId);

        // 如果数据库中没有这个策略，就没必要清理缓存了
        if (deletedRows == 0) {
            return;
        }

        // 3. 清理所有影响客户端的缓存
        // @CacheEvict注解已经清除了策略缓存，policies::#{policyId}
        if (!affectedClientIds.isEmpty()) {
            evictClientPoliciesCache(affectedClientIds);
        }
    }

    @Override
    @Cacheable(value = "clientPolicies", key = "#clientId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public List<Long> getClientPolicyIds(String clientId) {
        if (clientId == null) {
            return new ArrayList<>();
        }

        QueryWrapper<ClientPolicyMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("client_id", clientId).select("policy_id");
        return clientPolicyMappingMapper.selectList(queryWrapper)
                .stream()
                .map(ClientPolicyMapping::getPolicyId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CachePut(value = "policies", key = "#result.id", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Policy createDefaultPolicy(PolicyDto policyDto) {
        if (policyDto == null) {
            throw new IllegalArgumentException("策略数据不能为null");
        }

        // 检查是否已存在默认策略
        QueryWrapper<Policy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_default", true);
        if (policyMapper.selectCount(queryWrapper) > 0) {
            throw new IllegalArgumentException("默认策略已存在，不能重复创建");
        }

        Policy policy = convertDtoToEntity(policyDto);
        policy.setId(snowflakeIdGenerator.nextId());
        policy.setIsDefault(true);
        policy.setPriority(Integer.MAX_VALUE); // 默认策略优先级最低
        policy.setStatus("enabled"); // 默认策略默认启用
        policy.setVersion(String.valueOf(System.currentTimeMillis()));
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());

        policyMapper.insert(policy);
        return policy;
    }

    @Override
    @Cacheable(value = "clientPolicies", key = "'effective:' + #clientId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public Policy getEffectivePolicy(String clientId) {
        if (clientId == null) {
            return getDefaultPolicy();
        }

        // 1. 查找客户端最新激活的非默认策略
        QueryWrapper<ClientPolicyMapping> mappingQuery = new QueryWrapper<>();
        mappingQuery.eq("client_id", clientId)
                    .eq("is_active", true)
                    .orderByDesc("activated_at")
                    .last("LIMIT 1");
        
        ClientPolicyMapping activeMapping = clientPolicyMappingMapper.selectOne(mappingQuery);
        if (activeMapping != null) {
            Policy policy = policyMapper.selectById(activeMapping.getPolicyId());
            if (policy != null && "enabled".equals(policy.getStatus()) && !Boolean.TRUE.equals(policy.getIsDefault())) {
                return policy;
            }
        }

        // 2. 如果没有激活的非默认策略，返回默认策略
        return getDefaultPolicy();
    }

    @Override
    @Transactional
    @CacheEvict(value = "clientPolicies", allEntries = true, condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void activatePolicy(String clientId, Long policyId) {
        if (clientId == null || policyId == null) {
            return;
        }

        // 1. 检查策略是否存在
        Policy policy = policyMapper.selectById(policyId);
        if (policy == null) {
            throw new IllegalArgumentException("策略不存在: " + policyId);
        }

        // 2. 检查客户端是否已关联该策略
        QueryWrapper<ClientPolicyMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("client_id", clientId).eq("policy_id", policyId);
        ClientPolicyMapping mapping = clientPolicyMappingMapper.selectOne(queryWrapper);
        
        if (mapping == null) {
            throw new IllegalArgumentException("客户端未关联该策略");
        }

        // 3. 停用所有其他策略，激活指定策略
        deactivateAllClientPolicies(clientId);
        
        mapping.setIsActive(true);
        mapping.setActivatedAt(LocalDateTime.now());
        clientPolicyMappingMapper.updateById(mapping);
        
        // 策略激活后，异步预热客户端缓存
        preWarmClientCache(clientId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "policies", key = "#policyId", condition = "@cacheAvailabilityService.isCacheAvailable()")
    public void updatePolicyStatus(Long policyId, String status) {
        if (policyId == null || status == null) {
            throw new IllegalArgumentException("策略ID和状态不能为null");
        }

        // 验证状态值
        if (!"enabled".equals(status) && !"disabled".equals(status)) {
            throw new IllegalArgumentException("无效的策略状态: " + status);
        }

        // 检查策略是否存在
        Policy existingPolicy = policyMapper.selectById(policyId);
        if (existingPolicy == null) {
            throw new IllegalArgumentException("策略不存在: " + policyId);
        }

        // 检查是否为默认策略，默认策略不可修改
        if (Boolean.TRUE.equals(existingPolicy.getIsDefault())) {
            throw new IllegalArgumentException("默认策略不可修改状态");
        }

        // 更新策略状态
        Policy policy = new Policy();
        policy.setId(policyId);
        policy.setStatus(status);
        policy.setUpdatedAt(LocalDateTime.now());
        policyMapper.updateById(policy);

        // 如果策略被禁用，需要清理所有客户端的相关缓存
        if ("disabled".equals(status)) {
            // 查找所有使用该策略的客户端
            QueryWrapper<ClientPolicyMapping> mappingQuery = new QueryWrapper<>();
            mappingQuery.eq("policy_id", policyId).select("client_id");
            List<String> affectedClientIds = clientPolicyMappingMapper.selectList(mappingQuery)
                    .stream()
                    .map(ClientPolicyMapping::getClientId)
                    .distinct()
                    .collect(Collectors.toList());

            if (!affectedClientIds.isEmpty()) {
                evictClientPoliciesCache(affectedClientIds);
                // 异步预热受影响的客户端缓存
                preWarmClientsCaches(affectedClientIds);
            }
        }
    }

    /**
     * 停用客户端的所有策略
     */
    private void deactivateAllClientPolicies(String clientId) {
        if (clientId == null) {
            return;
        }
        
        QueryWrapper<ClientPolicyMapping> updateWrapper = new QueryWrapper<>();
        updateWrapper.eq("client_id", clientId).eq("is_active", true);
        
        List<ClientPolicyMapping> activeMappings = clientPolicyMappingMapper.selectList(updateWrapper);
        for (ClientPolicyMapping mapping : activeMappings) {
            mapping.setIsActive(false);
            clientPolicyMappingMapper.updateById(mapping);
        }
    }

    /**
     * 获取默认策略
     */
    @Cacheable(value = "policies", key = "'default'", condition = "@cacheAvailabilityService.isCacheAvailable()")
    private Policy getDefaultPolicy() {
        QueryWrapper<Policy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_default", true).eq("status", "enabled");
        return policyMapper.selectOne(queryWrapper);
    }

    /**
     * 异步预热单个客户端缓存
     * 根据项目规范，在策略变更后主动预热相关客户端缓存
     */
    private void preWarmClientCache(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return;
        }
        
        // 使用单独的线程进行异步预热，不影响主业务流程
        new Thread(() -> {
            try {
                heartbeatService.preWarmClientPolicyCache(clientId);
            } catch (Exception e) {
                // 预热失败不影响主业务，只记录日志
                // 在生产环境中应该使用日志框架记录
            }
        }, "policy-cache-prewarmer-" + clientId).start();
    }

    /**
     * 异步批量预热客户端缓存
     * 根据项目规范，在策略变更后主动预热相关客户端缓存
     */
    private void preWarmClientsCaches(List<String> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return;
        }
        
        // 使用单独的线程进行异步批量预热
        new Thread(() -> {
            for (String clientId : clientIds) {
                try {
                    heartbeatService.preWarmClientPolicyCache(clientId);
                    // 适当的间隔避免瞬时压力
                    Thread.sleep(10);
                } catch (Exception e) {
                    // 预热失败不影响主业务，继续处理下一个
                }
            }
        }, "policy-cache-batch-prewarmer").start();
    }

    /**
     * 预热受策略影响的所有客户端缓存
     * 根据项目规范，在策略变更后主动预热相关客户端缓存
     */
    private void preWarmAffectedClients(Long policyId, boolean isUpdate) {
        if (policyId == null) {
            return;
        }
        
        // 异步查找并预热受影响的客户端
        new Thread(() -> {
            try {
                // 查找所有使用该策略的客户端
                QueryWrapper<ClientPolicyMapping> mappingQuery = new QueryWrapper<>();
                mappingQuery.eq("policy_id", policyId).select("client_id");
                List<String> affectedClientIds = clientPolicyMappingMapper.selectList(mappingQuery)
                        .stream()
                        .map(ClientPolicyMapping::getClientId)
                        .distinct()
                        .collect(Collectors.toList());
                
                if (!affectedClientIds.isEmpty()) {
                    // 预热所有受影响的客户端
                    for (String clientId : affectedClientIds) {
                        try {
                            heartbeatService.preWarmClientPolicyCache(clientId);
                            // 适当间隔避免瞬时压力
                            Thread.sleep(5);
                        } catch (Exception e) {
                            // 单个客户端预热失败不影响其他客户端
                        }
                    }
                }
            } catch (Exception e) {
                // 预热失败不影响主业务流程
            }
        }, "policy-affected-clients-prewarmer-" + policyId).start();
    }

    /**
     * 清除多个客户端的策略缓存
     * @param clientIds 客户端ID列表
     */
    @CacheEvict(value = "clientPolicies", allEntries = false)
    private void evictClientPoliciesCache(List<String> clientIds) {
        // 这里使用编程式的缓存清除，因为@CacheEvict不支持动态key列表
        if (clientIds != null && !clientIds.isEmpty()) {
            // 使用RBatch进行批量清除
            RBatch batch = redissonClient.createBatch();
            for (String clientId : clientIds) {
                // 清除Spring Cache管理的缓存key
                String cacheKey = "clientPolicies::" + clientId;
                batch.getBucket(cacheKey).deleteAsync();
            }
            batch.execute();
        }
    }

    private Policy convertDtoToEntity(PolicyDto dto) {
        if (dto == null) {
            return null;
        }
        
        Policy entity = new Policy();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        entity.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);

        if (StringUtils.hasText(dto.getId())) {
            try {
                entity.setId(Long.parseLong(dto.getId()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("策略ID '" + dto.getId() + "' 不是有效的数字格式。");
            }
        }
        return entity;
    }


}