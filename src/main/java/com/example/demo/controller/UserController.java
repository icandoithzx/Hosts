package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ExternalUserDto;
import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String userId) {
        try {
            User user = userService.getById(userId);
            if (user != null) {
                return ResponseEntity.ok(ApiResponse.success(user));
            } else {
                return ResponseEntity.status(404).body(
                    ApiResponse.<User>errorWithType(404, "用户不存在")
                );
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，用户ID: {}", userId, e);
            return ResponseEntity.status(500).body(
                ApiResponse.<User>errorWithType(500, "获取用户信息失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取所有用户列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<List<User>>errorWithType(500, "获取用户列表失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 根据组织ID获取用户列表
     */
    @GetMapping("/org/{orgId}")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByOrgId(@PathVariable String orgId) {
        try {
            List<User> users = userService.getByOrgId(orgId);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("根据组织ID获取用户列表失败，组织ID: {}", orgId, e);
            return ResponseEntity.status(500).body(
                ApiResponse.<List<User>>errorWithType(500, "获取用户列表失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 根据用户等级获取用户列表
     */
    @GetMapping("/level/{mLevel}")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByLevel(@PathVariable Integer mLevel) {
        try {
            List<User> users = userService.getByMLevel(mLevel);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("根据用户等级获取用户列表失败，等级: {}", mLevel, e);
            return ResponseEntity.status(500).body(
                ApiResponse.<List<User>>errorWithType(500, "获取用户列表失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 手动触发用户数据同步（内部接口）
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> manualSync(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<ExternalUserDto> users = (List<ExternalUserDto>) request.get("users");
            String version = (String) request.get("version");
            
            if (users == null || users.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<String>errorWithType(400, "用户数据不能为空")
                );
            }
            
            if (version == null || version.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.<String>errorWithType(400, "版本号不能为空")
                );
            }
            
            boolean success = userService.syncFromExternal(users, version);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("用户数据同步成功"));
            } else {
                return ResponseEntity.status(500).body(
                    ApiResponse.<String>errorWithType(500, "用户数据同步失败")
                );
            }
        } catch (Exception e) {
            log.error("手动同步用户数据失败", e);
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
            boolean needSync = userService.needSync();
            String currentVersion = userService.getCurrentVersion();
            
            Map<String, Object> result = new HashMap<>();
            result.put("needSync", needSync);
            result.put("currentVersion", currentVersion != null ? currentVersion : "未知");
            result.put("message", needSync ? "需要同步用户数据" : "用户数据较新");
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("检查同步状态失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<Map<String, Object>>errorWithType(500, "检查同步状态失败: " + e.getMessage())
            );
        }
    }
    
    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = userService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return ResponseEntity.status(500).body(
                ApiResponse.<Map<String, Object>>errorWithType(500, "获取统计信息失败: " + e.getMessage())
            );
        }
    }
}