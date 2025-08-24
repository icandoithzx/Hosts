package com.example.demo.controller;

import com.example.demo.dto.ExternalOrganizationDto;
import com.example.demo.dto.OrganizationDto;
import com.example.demo.model.entity.Organization;
import com.example.demo.service.OrganizationService;
import com.example.demo.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 组织架构管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }
    
    /**
     * 获取组织架构树
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getOrganizationTree() {
        try {
            List<OrganizationDto> tree = organizationService.getOrganizationTree();
            return ResponseEntity.ok(ApiResponse.success(tree));
        } catch (Exception e) {
            log.error("获取组织架构树失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<List<OrganizationDto>>errorWithType(500, "获取组织架构树失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 根据ID获取组织信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Organization>> getById(@PathVariable String id) {
        try {
            Organization organization = organizationService.getById(id);
            if (organization == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(organization));
        } catch (Exception e) {
            log.error("获取组织信息失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<Organization>errorWithType(500, "获取组织信息失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 根据上级组织ID获取子组织
     */
    @GetMapping("/children/{parentId}")
    public ResponseEntity<ApiResponse<List<Organization>>> getChildren(@PathVariable String parentId) {
        try {
            List<Organization> children = organizationService.getByParentId(parentId);
            return ResponseEntity.ok(ApiResponse.success(children));
        } catch (Exception e) {
            log.error("获取子组织失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<List<Organization>>errorWithType(500, "获取子组织失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取所有组织的扁平列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Organization>>> getAllOrganizations() {
        try {
            List<Organization> organizations = organizationService.getAllOrganizations();
            return ResponseEntity.ok(ApiResponse.success(organizations));
        } catch (Exception e) {
            log.error("获取组织列表失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<List<Organization>>errorWithType(500, "获取组织列表失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 手动触发组织架构同步（内部接口）
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> manualSync(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<ExternalOrganizationDto> organizations = (List<ExternalOrganizationDto>) request.get("organizations");
            String version = (String) request.get("version");
            
            if (organizations == null || organizations.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<String>errorWithType(400, "组织数据不能为空")
                );
            }
            
            if (version == null || version.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<String>errorWithType(400, "版本号不能为空")
                );
            }
            
            boolean success = organizationService.syncFromExternal(organizations, version);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("组织架构同步成功"));
            } else {
                return ResponseEntity.status(500).body(
                    ApiResponse.<String>errorWithType(500, "组织架构同步失败")
                );
            }
        } catch (Exception e) {
            log.error("手动同步组织架构失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<String>errorWithType(500, "同步失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 检查是否需要同步
     */
    @GetMapping("/sync/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSyncStatus() {
        try {
            boolean needSync = organizationService.needSync();
            String currentVersion = organizationService.getCurrentVersion();
            
            Map<String, Object> result = Map.of(
                "needSync", needSync,
                "currentVersion", currentVersion != null ? currentVersion : "未知",
                "message", needSync ? "需要同步组织架构" : "组织架构数据较新"
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("检查同步状态失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<Map<String, Object>>errorWithType(500, "检查同步状态失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取组织架构统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = organizationService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取组织架构统计信息失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<Map<String, Object>>errorWithType(500, "获取统计信息失败: " + e.getMessage())
            );
        }
    }
}