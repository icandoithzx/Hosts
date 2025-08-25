package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.HostQueryDto;
import com.example.demo.model.entity.Host;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 主机注册管理 Mapper 接口
 */
@Mapper
public interface HostMapper extends BaseMapper<Host> {
    
    /**
     * 分页查询主机列表（支持JOIN查询用户名）
     * @param page 分页参数
     * @param queryDto 查询条件
     * @return 分页结果
     */
    IPage<Host> selectHostsWithUserPage(@Param("page") Page<Host> page, @Param("query") HostQueryDto queryDto);

}