package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("policies")
public class Policy implements Serializable {
    /**
     * 策略ID，使用雪花算法生成。
     * IdType.ASSIGN_ID 告诉MyBatis-Plus，ID是由我们自己的代码（雪花算法）在插入前赋值的。
     */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("id")
    private Long id;

    @TableField("name")
    private String name;
    
    @TableField("description")
    private String description;
    
    @TableField("status")
    private String status; // 'enabled' or 'disabled'
    
    @TableField("version")
    private String version;
    
    @TableField("is_default")
    private Boolean isDefault; // 是否为默认策略
    
    @TableField("priority")
    private Integer priority; // 优先级，数值越大优先级越高
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}