package com.fabriciot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    @TableField("role_id")
    private Long roleId;

    @TableField("permission_id")
    private Long permissionId;
}

