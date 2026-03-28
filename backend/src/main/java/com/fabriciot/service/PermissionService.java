package com.fabriciot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.common.result.PageQuery;
import com.fabriciot.dto.permission.PermissionRoleCreateRequest;
import com.fabriciot.dto.permission.PermissionRoleUpdateRequest;
import com.fabriciot.dto.permission.PermissionUserCreateRequest;
import com.fabriciot.dto.permission.PermissionUserUpdateRequest;
import com.fabriciot.vo.permission.PermissionRoleVO;
import com.fabriciot.vo.permission.PermissionTreeVO;
import com.fabriciot.vo.permission.PermissionUserVO;

import java.util.List;

public interface PermissionService {

    IPage<PermissionUserVO> users(PageQuery query);

    PermissionUserVO createUser(PermissionUserCreateRequest request);

    PermissionUserVO updateUser(Long userId, PermissionUserUpdateRequest request);

    void deleteUser(Long userId);

    IPage<PermissionRoleVO> roles(PageQuery query);

    PermissionRoleVO createRole(PermissionRoleCreateRequest request);

    PermissionRoleVO updateRole(Long roleId, PermissionRoleUpdateRequest request);

    void deleteRole(Long roleId);

    List<PermissionTreeVO> tree();

    List<String> rolePermissions(Long roleId);

    void saveRolePermissions(Long roleId, List<String> permissionCodes);
}

