package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织架构 Mapper 接口
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {
    
    /**
     * 根据上级组织ID查询子组织
     * @param parentId 上级组织ID
     * @return 子组织列表
     */
    List<Organization> selectByParentId(@Param("parentId") String parentId);
    
    /**
     * 查询所有根级别组织
     * @return 根级别组织列表
     */
    List<Organization> selectRootOrganizations();
    
    /**
     * 批量删除所有组织（用于全量同步前清空）
     */
    void deleteAll();
    
    /**
     * 查询组织总数
     * @return 总数
     */
    Long countAll();
}