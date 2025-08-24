package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    @Select("SELECT * FROM organizations WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_order, name")
    List<Organization> selectByParentId(@Param("parentId") Long parentId);
    
    /**
     * 查询所有根级别组织
     * @return 根级别组织列表
     */
    @Select("SELECT * FROM organizations WHERE parent_id = 0 AND status = 1 ORDER BY sort_order, name")
    List<Organization> selectRootOrganizations();
    
    /**
     * 根据路径查询所有子组织（包含自己）
     * @param path 组织路径
     * @return 组织列表
     */
    @Select("SELECT * FROM organizations WHERE (path = #{path} OR path LIKE CONCAT(#{path}, '/%')) AND status = 1 ORDER BY level, sort_order, name")
    List<Organization> selectByPathPrefix(@Param("path") String path);
    
    /**
     * 批量删除所有组织（用于全量同步前清空）
     */
    @Select("DELETE FROM organizations")
    void deleteAll();
    
    /**
     * 查询组织总数
     * @return 总数
     */
    @Select("SELECT COUNT(*) FROM organizations")
    Long countAll();
}