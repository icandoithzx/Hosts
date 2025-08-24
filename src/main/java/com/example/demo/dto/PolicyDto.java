package com.example.demo.dto;

import lombok.Data;

/**
 * 策略数据传输对象 (DTO)。
 * 用于在API层接收和发送策略数据，与数据库实体(Policy)解耦。
 * 将id定义为String类型，以增加API的灵活性，并由后端进行验证和转换。
 */
@Data
public class PolicyDto {
    private String id;
    private String name;
    private String description;
    private String status;
    private Boolean isDefault; // 是否为默认策略
    private Integer priority; // 优先级
    // DTO中通常不需要version, createdAt, updatedAt等由后端控制的字段
}
