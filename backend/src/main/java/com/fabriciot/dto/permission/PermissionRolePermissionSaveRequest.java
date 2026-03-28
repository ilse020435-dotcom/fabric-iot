package com.fabriciot.dto.permission;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PermissionRolePermissionSaveRequest {

    @NotNull(message = "权限列表不能为空")
    private List<String> permissionCodes;
}
