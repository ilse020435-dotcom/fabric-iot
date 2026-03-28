package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fabriciot.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.*
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.deleted = 0
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(1)
            FROM sys_role_permission rp
            INNER JOIN sys_permission p ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
              AND p.deleted = 0
            """)
    Long countPermissionByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_role
            WHERE role_id = #{roleId}
            """)
    Long countUserByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT *
            FROM sys_role
            WHERE role_code = #{roleCode}
            LIMIT 1
            """)
    SysRole findAnyByRoleCode(@Param("roleCode") String roleCode);
}

