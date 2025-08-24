package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.Policy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PolicyMapper extends BaseMapper<Policy> {
}
