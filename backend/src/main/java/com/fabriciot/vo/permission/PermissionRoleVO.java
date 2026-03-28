package com.fabriciot.vo.permission;

import lombok.Data;

@Data
public class PermissionRoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String roleDesc;

    private Long permissionCount;
}

