package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fabriciot.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT *
            FROM sys_user
            WHERE username = #{username}
              AND deleted = 0
            LIMIT 1
            """)
    SysUser findByUsername(@Param("username") String username);

    @Select("""
            SELECT *
            FROM sys_user
            WHERE username = #{username}
            LIMIT 1
            """)
    SysUser findAnyByUsername(@Param("username") String username);
}

