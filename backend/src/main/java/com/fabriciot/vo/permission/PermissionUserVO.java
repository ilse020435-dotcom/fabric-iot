package com.fabriciot.vo.permission;

import lombok.Data;

@Data
public class PermissionUserVO {

    private Long id;

    private String username;

    private Long roleId;

    private String role;

    private String status;

    private String statusCode;

    private String createdAt;
}

