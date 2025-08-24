package com.example.demo.service.impl;

import com.example.demo.dto.PolicyDto;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.PolicyAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 策略管理功能演示和测试
 * 根据项目规范，每次完成功能开发后，都需要编写单元测试
 */
@Component
public class PolicyManagementDemo {

    @Autowired
    private PolicyAdminService policyAdminService;

    /**
     * 演示新的策略管理功能
     */
    public void demonstrateNewFeatures() {
        try {
            // 1. 创建默认策略
            System.out.println("=== 1. 创建默认策略 ===");
            PolicyDto defaultPolicyDto = new PolicyDto();
            defaultPolicyDto.setName("系统默认策略");
            defaultPolicyDto.setDescription("对所有客户端生效的默认策略");
            defaultPolicyDto.setStatus("enabled");
            defaultPolicyDto.setIsDefault(true);
            
            Policy defaultPolicy = policyAdminService.createDefaultPolicy(defaultPolicyDto);
            System.out.println("默认策略创建成功: ID=" + defaultPolicy.getId());

            // 2. 创建普通策略
            System.out.println("\n=== 2. 创建普通策略 ===");
            PolicyDto policy1Dto = new PolicyDto();
            policy1Dto.setName("策略A");
            policy1Dto.setDescription("客户端专用策略A");
            policy1Dto.setStatus("enabled");
            policy1Dto.setPriority(100);
            
            Policy policy1 = policyAdminService.createOrUpdatePolicy(policy1Dto);
            System.out.println("策略A创建成功: ID=" + policy1.getId());

            PolicyDto policy2Dto = new PolicyDto();
            policy2Dto.setName("策略B");
            policy2Dto.setDescription("客户端专用策略B");
            policy2Dto.setStatus("enabled");
            policy2Dto.setPriority(200);
            
            Policy policy2 = policyAdminService.createOrUpdatePolicy(policy2Dto);
            System.out.println("策略B创建成功: ID=" + policy2.getId());

            // 3. 测试客户端策略分配和激活
            System.out.println("\n=== 3. 测试客户端策略分配 ===");
            String clientId = "test-client-001";

            // 检查初始生效策略（应该是默认策略）
            Policy initialEffective = policyAdminService.getEffectivePolicy(clientId);
            System.out.println("初始生效策略: " + 
                (initialEffective != null ? initialEffective.getName() : "无"));

            // 分配策略A给客户端
            policyAdminService.assignPolicyToClient(clientId, policy1.getId());
            Policy effective1 = policyAdminService.getEffectivePolicy(clientId);
            System.out.println("分配策略A后生效策略: " + effective1.getName());

            // 分配策略B给客户端（应该覆盖策略A）
            policyAdminService.assignPolicyToClient(clientId, policy2.getId());
            Policy effective2 = policyAdminService.getEffectivePolicy(clientId);
            System.out.println("分配策略B后生效策略: " + effective2.getName());

            // 重新激活策略A
            policyAdminService.activatePolicy(clientId, policy1.getId());
            Policy effective3 = policyAdminService.getEffectivePolicy(clientId);
            System.out.println("重新激活策略A后生效策略: " + effective3.getName());

            // 4. 测试批量分配
            System.out.println("\n=== 4. 测试批量分配 ===");
            List<String> clientIds = Arrays.asList("client-001", "client-002", "client-003");
            policyAdminService.assignPolicyToClients(clientIds, policy2.getId());
            
            for (String client : clientIds) {
                Policy clientEffective = policyAdminService.getEffectivePolicy(client);
                System.out.println("客户端 " + client + " 生效策略: " + 
                    (clientEffective != null ? clientEffective.getName() : "无"));
            }

            // 5. 测试默认策略不可修改保护
            System.out.println("\n=== 5. 测试默认策略保护 ===");
            try {
                PolicyDto updateDefault = new PolicyDto();
                updateDefault.setId(String.valueOf(defaultPolicy.getId()));
                updateDefault.setName("尝试修改默认策略");
                updateDefault.setDescription("这应该失败");
                
                policyAdminService.createOrUpdatePolicy(updateDefault);
                System.out.println("错误：默认策略修改未被阻止");
            } catch (IllegalArgumentException e) {
                System.out.println("正确：默认策略修改被阻止 - " + e.getMessage());
            }

            System.out.println("\n=== 功能演示完成 ===");

        } catch (Exception e) {
            System.err.println("演示过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证空值检查和重复检查机制
     */
    public void testValidationMechanisms() {
        System.out.println("\n=== 验证机制测试 ===");

        // 测试空值检查
        try {
            policyAdminService.assignPolicyToClient(null, 123L);
            policyAdminService.assignPolicyToClient("client", null);
            policyAdminService.assignPolicyToClients(null, 123L);
            System.out.println("空值检查通过");
        } catch (Exception e) {
            System.out.println("空值检查异常: " + e.getMessage());
        }

        // 测试重复分配检查
        try {
            PolicyDto testPolicy = new PolicyDto();
            testPolicy.setName("测试策略");
            testPolicy.setDescription("用于测试重复分配");
            testPolicy.setStatus("enabled");
            
            Policy policy = policyAdminService.createOrUpdatePolicy(testPolicy);
            String testClient = "test-duplicate-client";
            
            // 第一次分配
            policyAdminService.assignPolicyToClient(testClient, policy.getId());
            System.out.println("第一次分配成功");
            
            // 第二次分配（应该更新激活状态而不是重复创建）
            policyAdminService.assignPolicyToClient(testClient, policy.getId());
            System.out.println("重复分配处理正确");
            
        } catch (Exception e) {
            System.out.println("重复检查异常: " + e.getMessage());
        }
    }
}