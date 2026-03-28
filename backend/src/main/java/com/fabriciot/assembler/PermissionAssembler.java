package com.fabriciot.assembler;

import com.fabriciot.entity.SysPermission;
import com.fabriciot.entity.SysRole;
import com.fabriciot.entity.SysUser;
import com.fabriciot.vo.permission.PermissionRoleVO;
import com.fabriciot.vo.permission.PermissionTreeVO;
import com.fabriciot.vo.permission.PermissionUserVO;
import org.springframework.stereotype.Component;

@Component
public class PermissionAssembler {

    private final AssemblerSupport support;

    public PermissionAssembler(AssemblerSupport support) {
        this.support = support;
    }

    public PermissionUserVO toUserVO(SysUser user, Long roleId, String roleName) {
        PermissionUserVO vo = new PermissionUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRoleId(roleId);
        vo.setRole(roleName == null ? "" : roleName);
        vo.setStatus(support.userStatusLabel(user.getStatus()));
        vo.setStatusCode(user.getStatus());
        vo.setCreatedAt(support.format(user.getCreatedAt()));
        return vo;
    }

    public PermissionRoleVO toRoleVO(SysRole role, Long permissionCount) {
        PermissionRoleVO vo = new PermissionRoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleDesc(role.getRoleDesc());
        vo.setPermissionCount(permissionCount);
        return vo;
    }

    public PermissionTreeVO toTreeVO(SysPermission permission) {
        PermissionTreeVO vo = new PermissionTreeVO();
        vo.setKey(permission.getPermCode());
        vo.setLabel(permission.getPermName());
        vo.setType(permission.getType());
        return vo;
    }
}

