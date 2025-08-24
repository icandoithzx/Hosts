package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.Host;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主机注册管理 Mapper 接口
 */
@Mapper
public interface HostMapper extends BaseMapper<Host> {

}