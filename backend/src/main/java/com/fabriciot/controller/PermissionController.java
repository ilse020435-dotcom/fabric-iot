package com.fabriciot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.common.result.PageQuery;
import com.fabriciot.common.result.Result;
import com.fabriciot.dto.permission.PermissionRoleCreateRequest;
import com.fabriciot.dto.permission.PermissionRolePermissionSaveRequest;
import com.fabriciot.dto.permission.PermissionRoleUpdateRequest;
import com.fabriciot.dto.permission.PermissionUserCreateRequest;
import com.fabriciot.dto.permission.PermissionUserUpdateRequest;
import com.fabriciot.service.PermissionService;
import com.fabriciot.vo.permission.PermissionRoleVO;
import com.fabriciot.vo.permission.PermissionTreeVO;
import com.fabriciot.vo.permission.PermissionUserVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<List<PermissionUserVO>> users(@Valid PageQuery query) {
        IPage<PermissionUserVO> page = permissionService.users(query);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<PermissionUserVO> createUser(@Valid @RequestBody PermissionUserCreateRequest request) {
        return Result.success(permissionService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<PermissionUserVO> updateUser(@PathVariable("id") Long userId,
                                               @Valid @RequestBody PermissionUserUpdateRequest request) {
        return Result.success(permissionService.updateUser(userId, request));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('user:manage')")
    public Result<Void> deleteUser(@PathVariable("id") Long userId) {
        permissionService.deleteUser(userId);
        return Result.success(null);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('role:manage','user:manage')")
    public Result<List<PermissionRoleVO>> roles(@Valid PageQuery query) {
        IPage<PermissionRoleVO> page = permissionService.roles(query);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<PermissionRoleVO> createRole(@Valid @RequestBody PermissionRoleCreateRequest request) {
        return Result.success(permissionService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<PermissionRoleVO> updateRole(@PathVariable("id") Long roleId,
                                               @Valid @RequestBody PermissionRoleUpdateRequest request) {
        return Result.success(permissionService.updateRole(roleId, request));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<Void> deleteRole(@PathVariable("id") Long roleId) {
        permissionService.deleteRole(roleId);
        return Result.success(null);
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('role:manage','user:manage')")
    public Result<List<PermissionTreeVO>> tree() {
        return Result.success(permissionService.tree());
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<List<String>> rolePermissions(@PathVariable("id") Long roleId) {
        return Result.success(permissionService.rolePermissions(roleId));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    public Result<Void> saveRolePermissions(@PathVariable("id") Long roleId,
                                            @Valid @RequestBody PermissionRolePermissionSaveRequest request) {
        permissionService.saveRolePermissions(roleId, request.getPermissionCodes());
        return Result.success(null);
    }
}

