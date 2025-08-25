package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.dto.ExternalUserDto;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    @Override
    public User getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return userMapper.selectById(id);
    }
    
    @Override
    public List<User> getByOrgId(String orgId) {
        return userMapper.selectByOrgId(orgId);
    }
    
    @Override
    public List<User> getByMLevel(Integer mLevel) {
        return userMapper.selectByMLevel(mLevel);
    }
    
    @Override
    public List<User> getAllUsers() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("m_level").orderByAsc("name");
        return userMapper.selectList(queryWrapper);
    }
    
    @Override
    @Transactional
    public boolean syncFromExternal(List<ExternalUserDto> externalUsers, String version) {
        try {
            log.info("🔄 开始同步用户数据，版本: {}, 用户数量: {}", version, 
                    externalUsers != null ? externalUsers.size() : 0);
            
            if (externalUsers == null || externalUsers.isEmpty()) {
                log.warn("⚠️ 外部用户数据为空，跳过同步");
                return false;
            }
            
            // 1. 清空现有数据
            userMapper.deleteAll();
            log.info("🗑️ 已清空现有用户数据");
            
            // 2. 转换外部数据为内部实体
            List<User> users = convertExternalToInternal(externalUsers);
            
            // 3. 批量插入新数据
            for (User user : users) {
                userMapper.insert(user);
            }
            
            log.info("✅ 用户数据同步完成，成功插入 {} 条记录", users.size());
            return true;
            
        } catch (Exception e) {
            log.error("❌ 用户数据同步失败: {}", e.getMessage(), e);
            throw new RuntimeException("用户数据同步失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean needSync() {
        // 简化同步策略：如果数据库为空，需要同步
        Long count = userMapper.countAll();
        if (count == 0) {
            log.info("📋 用户数据为空，需要同步");
            return true;
        }
        
        log.debug("✨ 用户数据存在，暂不需要同步");
        return false;
    }
    
    @Override
    public String getCurrentVersion() {
        // 简化版本不再跟踪外部版本号
        return "simplified-v1.0.0";
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总用户数
        Long totalCount = userMapper.countAll();
        statistics.put("totalCount", totalCount);
        
        // 按等级统计
        List<User> allUsers = getAllUsers();
        Map<Integer, Long> levelStats = allUsers.stream()
                .collect(Collectors.groupingBy(User::getMLevel, Collectors.counting()));
        statistics.put("levelStatistics", levelStats);
        
        log.info("📊 用户统计信息 - 总数: {}, 等级分布: {}", totalCount, levelStats);
        return statistics;
    }
    
    /**
     * 转换外部数据为内部实体
     */
    private List<User> convertExternalToInternal(List<ExternalUserDto> externalUsers) {
        return externalUsers.stream()
                .map(external -> {
                    User user = new User();
                    BeanUtils.copyProperties(external, user);
                    
                    // 设置默认值
                    if (user.getMLevel() == null) {
                        user.setMLevel(0);
                    }
                    
                    return user;
                })
                .collect(Collectors.toList());
    }
}