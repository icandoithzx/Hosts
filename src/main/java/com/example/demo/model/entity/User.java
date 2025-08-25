package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户实体类
 * 从外部系统同步的用户数据
 */
@Data
@TableName("users")
public class User implements Serializable {
    
    /**
     * 用户ID
     */
    @TableId(type = IdType.INPUT)
    @TableField("id")
    private String id;
    
    /**
     * 组织架构ID
     */
    @TableField("org_id")
    private String orgId;
    
    /**
     * 用户名称
     */
    @TableField("name")
    private String name;
    
    /**
     * 组织架构的全名称
     */
    @TableField("org_name")
    private String orgName;
    
    /**
     * 用户的等级
     */
    @TableField("m_level")
    private Integer mLevel;
}