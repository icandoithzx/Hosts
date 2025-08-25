package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据组织ID查询用户
     * @param orgId 组织ID
     * @return 用户列表
     */
    List<User> selectByOrgId(@Param("orgId") String orgId);
    
    /**
     * 根据用户等级查询用户
     * @param mLevel 用户等级
     * @return 用户列表
     */
    List<User> selectByMLevel(@Param("mLevel") Integer mLevel);
    
    /**
     * 批量删除所有用户（用于全量同步前清空）
     */
    void deleteAll();
    
    /**
     * 查询用户总数
     * @return 总数
     */
    Long countAll();
}