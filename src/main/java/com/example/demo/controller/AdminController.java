package com.example.demo.controller;

import com.example.demo.dto.DeletePolicyRequestDto;
import com.example.demo.dto.PolicyDto;
import com.example.demo.model.entity.Policy;
import com.example.demo.service.PolicyAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端控制器，用于策略的创建、更新、分配和删除。
 * 所有写操作均使用POST方法。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PolicyAdminService policyAdminService;

    public AdminController(PolicyAdminService policyAdminService) {
        this.policyAdminService = policyAdminService;
    }

    @PostMapping("/client/{clientId}/assign/{policyId}")
    public String assignPolicyToClient(@PathVariable String clientId, @PathVariable Long policyId) {
        policyAdminService.assignPolicyToClient(clientId, policyId);
        return "OK";
    }

    /**
     * 批量为多个客户端分配同一个策略。
     * @param clientIdsList 客户端ID列表
     * @param policyId 策略ID
     * @return 操作结果
     */
    @PostMapping("/clients/assign/{policyId}")
    public String assignPolicyToClients(@RequestBody List<String> clientIdsList, @PathVariable Long policyId) {
        policyAdminService.assignPolicyToClients(clientIdsList, policyId);
        return "OK";
    }

    @PostMapping("/policy")
    public Policy createOrUpdatePolicy(@RequestBody PolicyDto policyDto) {
        return policyAdminService.createOrUpdatePolicy(policyDto);
    }

    /**
     * 使用POST方法删除一个策略。
     * @param request 包含要删除的策略ID的请求体
     * @return 成功时返回 HTTP 204 No Content
     */
    @PostMapping("/policy/delete")
    public ResponseEntity<Void> deletePolicy(@RequestBody DeletePolicyRequestDto request) {
        policyAdminService.deletePolicy(request.getPolicyId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 创建默认策略。
     * 默认策略对所有客户端生效，且无法修改。
     * @param policyDto 策略数据
     * @return 创建的默认策略
     */
    @PostMapping("/policy/default")
    public Policy createDefaultPolicy(@RequestBody PolicyDto policyDto) {
        return policyAdminService.createDefaultPolicy(policyDto);
    }

    /**
     * 获取客户端当前生效的策略。
     * @param clientId 客户端ID
     * @return 生效的策略
     */
    @GetMapping("/client/{clientId}/effective-policy")
    public ResponseEntity<Policy> getEffectivePolicy(@PathVariable String clientId) {
        Policy policy = policyAdminService.getEffectivePolicy(clientId);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    /**
     * 开启策略。
     * @param policyId 要开启的策略ID
     * @return 操作结果
     */
    @PostMapping("/policy/{policyId}/enable")
    public ResponseEntity<String> enablePolicy(@PathVariable Long policyId) {
        try {
            policyAdminService.updatePolicyStatus(policyId, "enabled");
            return ResponseEntity.ok("Policy enabled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to enable policy: " + e.getMessage());
        }
    }

    /**
     * 关闭策略。
     * @param policyId 要关闭的策略ID
     * @return 操作结果
     */
    @PostMapping("/policy/{policyId}/disable")
    public ResponseEntity<String> disablePolicy(@PathVariable Long policyId) {
        try {
            policyAdminService.updatePolicyStatus(policyId, "disabled");
            return ResponseEntity.ok("Policy disabled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to disable policy: " + e.getMessage());
        }
    }
}
