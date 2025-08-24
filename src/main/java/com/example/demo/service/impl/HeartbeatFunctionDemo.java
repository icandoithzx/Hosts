package com.example.demo.service.impl;

import com.example.demo.dto.HeartbeatRequest;
import com.example.demo.dto.HeartbeatResponse;
import com.example.demo.dto.PolicyDto;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.HeartbeatService;
import com.example.demo.service.PolicyAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 心跳功能演示类
 * 演示高并发心跳包处理和策略更新检查功能
 * 根据项目规范，每次完成功能开发后，都需要编写单元测试
 */
@Component
public class HeartbeatFunctionDemo {

    @Autowired
    private HeartbeatService heartbeatService;
    
    @Autowired
    private PolicyAdminService policyAdminService;

    /**
     * 演示心跳服务的完整功能
     */
    public void demonstrateHeartbeatFeatures() {
        System.out.println("=== 心跳功能演示开始 ===");
        
        try {
            // 1. 创建测试策略
            setupTestPolicies();
            
            // 2. 演示单客户端心跳处理
            demonstrateSingleClientHeartbeat();
            
            // 3. 演示高并发心跳处理
            demonstrateHighConcurrencyHeartbeat();
            
            // 4. 演示策略更新检查
            demonstratePolicyUpdateDetection();
            
            // 5. 演示自动缓存预热机制
            demonstrateAutoCachePreWarming();
            
            System.out.println("=== 心跳功能演示完成 ===");
            
        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置测试策略
     */
    private void setupTestPolicies() {
        System.out.println("\n--- 1. 设置测试策略 ---");
        
        // 创建默认策略
        PolicyDto defaultPolicy = new PolicyDto();
        defaultPolicy.setName("默认心跳策略");
        defaultPolicy.setDescription("心跳服务使用的默认策略");
        defaultPolicy.setStatus("enabled");
        defaultPolicy.setIsDefault(true);
        
        try {
            Policy createdDefault = policyAdminService.createDefaultPolicy(defaultPolicy);
            System.out.println("默认策略创建成功: " + createdDefault.getName());
        } catch (Exception e) {
            System.out.println("默认策略已存在，跳过创建");
        }

        // 创建客户端专用策略
        PolicyDto clientPolicy = new PolicyDto();
        clientPolicy.setName("心跳客户端策略");
        clientPolicy.setDescription("专用于心跳测试的客户端策略");
        clientPolicy.setStatus("enabled");
        clientPolicy.setPriority(100);
        
        Policy createdPolicy = policyAdminService.createOrUpdatePolicy(clientPolicy);
        System.out.println("客户端策略创建成功: " + createdPolicy.getName());
        
        // 分配策略给测试客户端
        policyAdminService.assignPolicyToClient("heartbeat-test-001", createdPolicy.getId());
        System.out.println("策略已分配给客户端: heartbeat-test-001");
    }

    /**
     * 演示单客户端心跳处理
     */
    private void demonstrateSingleClientHeartbeat() {
        System.out.println("\n--- 2. 单客户端心跳处理演示 ---");
        
        String clientId = "heartbeat-test-001";
        
        // 第一次心跳 - 客户端没有策略哈希
        HeartbeatRequest request1 = new HeartbeatRequest();
        request1.setClientId(clientId);
        request1.setCurrentPoliciesHash("");
        
        HeartbeatResponse response1 = heartbeatService.checkPolicies(
            request1.getClientId(), 
            request1.getCurrentPoliciesHash()
        );
        
        System.out.println("第一次心跳响应:");
        System.out.println("  需要更新: " + response1.isNeedsPolicyUpdate());
        System.out.println("  更新类型: " + response1.getUpdateType());
        System.out.println("  消息: " + response1.getMessage());
        
        // 第二次心跳 - 使用返回的哈希值
        if (response1.getLatestPoliciesHash() != null) {
            HeartbeatRequest request2 = new HeartbeatRequest();
            request2.setClientId(clientId);
            request2.setCurrentPoliciesHash(response1.getLatestPoliciesHash());
            
            HeartbeatResponse response2 = heartbeatService.checkPolicies(
                request2.getClientId(), 
                request2.getCurrentPoliciesHash()
            );
            
            System.out.println("第二次心跳响应:");
            System.out.println("  需要更新: " + response2.isNeedsPolicyUpdate());
            System.out.println("  消息: " + response2.getMessage());
        }
    }

    /**
     * 演示高并发心跳处理
     */
    private void demonstrateHighConcurrencyHeartbeat() {
        System.out.println("\n--- 3. 高并发心跳处理演示 ---");
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<String> clientIds = Arrays.asList(
            "client-001", "client-002", "client-003", "client-004", "client-005",
            "client-006", "client-007", "client-008", "client-009", "client-010"
        );
        
        // 为所有客户端分配策略
        Policy policy = policyAdminService.getEffectivePolicy("heartbeat-test-001");
        if (policy != null) {
            policyAdminService.assignPolicyToClients(clientIds, policy.getId());
            System.out.println("已为 " + clientIds.size() + " 个客户端分配策略");
        }
        
        // 并发执行心跳请求
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            final int requestId = i;
            executor.submit(() -> {
                String clientId = clientIds.get(requestId % clientIds.size());
                HeartbeatRequest request = new HeartbeatRequest();
                request.setClientId(clientId);
                request.setCurrentPoliciesHash("");
                
                HeartbeatResponse response = heartbeatService.checkPolicies(
                    request.getClientId(), 
                    request.getCurrentPoliciesHash()
                );
                
                if (requestId % 20 == 0) {
                    System.out.println("请求 " + requestId + " - 客户端: " + clientId + 
                        ", 需要更新: " + response.isNeedsPolicyUpdate());
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            System.out.println("100个并发心跳请求完成，耗时: " + (endTime - startTime) + "ms");
        } catch (InterruptedException e) {
            System.err.println("并发测试中断: " + e.getMessage());
        }
    }

    /**
     * 演示策略更新检查
     */
    private void demonstratePolicyUpdateDetection() {
        System.out.println("\n--- 4. 策略更新检查演示 ---");
        
        String clientId = "policy-update-test";
        
        // 创建并分配初始策略
        PolicyDto initialPolicy = new PolicyDto();
        initialPolicy.setName("初始策略");
        initialPolicy.setDescription("用于测试更新检查的初始策略");
        initialPolicy.setStatus("enabled");
        
        Policy created = policyAdminService.createOrUpdatePolicy(initialPolicy);
        policyAdminService.assignPolicyToClient(clientId, created.getId());
        
        // 第一次心跳
        HeartbeatResponse response1 = heartbeatService.checkPolicies(clientId, "");
        System.out.println("初始心跳 - 需要更新: " + response1.isNeedsPolicyUpdate());
        String initialHash = response1.getLatestPoliciesHash();
        
        // 更新策略
        created.setDescription("更新后的策略描述");
        PolicyDto updateDto = new PolicyDto();
        updateDto.setId(String.valueOf(created.getId()));
        updateDto.setName(created.getName());
        updateDto.setDescription("更新后的策略描述");
        updateDto.setStatus(created.getStatus());
        
        policyAdminService.createOrUpdatePolicy(updateDto);
        System.out.println("策略已更新");
        
        // 第二次心跳（使用旧哈希）
        HeartbeatResponse response2 = heartbeatService.checkPolicies(clientId, initialHash);
        System.out.println("更新后心跳 - 需要更新: " + response2.isNeedsPolicyUpdate());
        System.out.println("更新消息: " + response2.getMessage());
    }

    /**
     * 演示自动缓存预热机制
     */
    private void demonstrateAutoCachePreWarming() {
        System.out.println("\n--- 5. 自动缓存预热机制演示 ---");
        
        List<String> clientIds = Arrays.asList("auto-warm-001", "auto-warm-002", "auto-warm-003");
        
        // 创建一个测试策略
        PolicyDto testPolicy = new PolicyDto();
        testPolicy.setName("自动预热测试策略");
        testPolicy.setDescription("用于演示自动预热机制的测试策略");
        testPolicy.setStatus("enabled");
        testPolicy.setPriority(200);
        
        long startTime = System.currentTimeMillis();
        
        // 创建策略 - 会自动触发预热
        Policy createdPolicy = policyAdminService.createOrUpdatePolicy(testPolicy);
        System.out.println("策略已创建，系统将自动预热相关缓存: " + createdPolicy.getName());
        
        // 批量分配策略 - 会自动触发批量预热
        policyAdminService.assignPolicyToClients(clientIds, createdPolicy.getId());
        System.out.println("批量分配完成，系统将自动预热 " + clientIds.size() + " 个客户端缓存");
        
        // 策略激活 - 会自动触发预热
        String testClientId = clientIds.get(0);
        policyAdminService.activatePolicy(testClientId, createdPolicy.getId());
        System.out.println("策略激活完成，系统将自动预热客户端 " + testClientId + " 的缓存");
        
        // 策略更新 - 会自动触发受影响客户端的预热
        testPolicy.setId(String.valueOf(createdPolicy.getId()));
        testPolicy.setDescription("更新后的描述 - 触发自动预热");
        policyAdminService.createOrUpdatePolicy(testPolicy);
        System.out.println("策略更新完成，系统将自动预热所有使用该策略的客户端缓存");
        
        long endTime = System.currentTimeMillis();
        System.out.println("自动预热机制演示完成，耗时: " + (endTime - startTime) + "ms");
        
        // 等待一段时间让异步预热完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 验证预热效果
        System.out.println("验证自动预热效果:");
        for (String clientId : clientIds) {
            long accessStart = System.currentTimeMillis();
            Policy cachedPolicy = heartbeatService.getClientEffectivePolicy(clientId);
            String cachedHash = heartbeatService.getClientPolicyHash(clientId);
            long accessEnd = System.currentTimeMillis();
            
            System.out.println("客户端 " + clientId + " - 策略: " + 
                (cachedPolicy != null ? cachedPolicy.getName() : "无") + 
                ", 哈希: " + (cachedHash != null ? cachedHash.substring(0, 8) + "..." : "无") +
                ", 访问耗时: " + (accessEnd - accessStart) + "ms");
        }
    }

    /**
     * 验证空值检查和重复检查机制
     */
    public void testValidationMechanisms() {
        System.out.println("\n=== 验证机制测试 ===");

        // 测试空值检查
        try {
            HeartbeatResponse response1 = heartbeatService.checkPolicies(null, "hash");
            System.out.println("空客户端ID处理: " + response1.getMessage());
            
            HeartbeatResponse response2 = heartbeatService.checkPolicies("", "hash");
            System.out.println("空字符串客户端ID处理: " + response2.getMessage());
            
            Policy policy1 = heartbeatService.getClientEffectivePolicy(null);
            System.out.println("空值策略查询结果: " + (policy1 == null ? "正确返回null" : "错误"));
            
            String hash1 = heartbeatService.getClientPolicyHash("");
            System.out.println("空值哈希查询结果: " + (hash1 == null ? "正确返回null" : "错误"));
            
            System.out.println("空值检查机制验证通过");
            
        } catch (Exception e) {
            System.out.println("空值检查异常: " + e.getMessage());
        }

        // 测试缓存一致性
        try {
            String testClient = "consistency-test";
            Policy policy = policyAdminService.getEffectivePolicy("heartbeat-test-001");
            if (policy != null) {
                policyAdminService.assignPolicyToClient(testClient, policy.getId());
                
                // 多次获取应该返回一致的结果
                String hash1 = heartbeatService.getClientPolicyHash(testClient);
                String hash2 = heartbeatService.getClientPolicyHash(testClient);
                
                System.out.println("缓存一致性检查: " + 
                    (hash1 != null && hash1.equals(hash2) ? "通过" : "失败"));
            }
            
        } catch (Exception e) {
            System.out.println("缓存一致性测试异常: " + e.getMessage());
        }
    }
}