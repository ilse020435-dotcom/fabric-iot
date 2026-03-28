package com.fabriciot.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionUserUpdateRequest {

    @Size(min = 6, max = 64, message = "密码长度应在6到64之间")
    private String password;

    @NotNull(message = "请选择角色")
    private Long roleId;

    @NotBlank(message = "状态不能为空")
    private String status;
}
