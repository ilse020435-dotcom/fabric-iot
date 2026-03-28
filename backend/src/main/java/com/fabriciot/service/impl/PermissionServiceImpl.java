package com.fabriciot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fabriciot.assembler.PermissionAssembler;
import com.fabriciot.common.enums.UserStatus;
import com.fabriciot.common.result.PageQuery;
import com.fabriciot.dto.permission.PermissionRoleCreateRequest;
import com.fabriciot.dto.permission.PermissionRoleUpdateRequest;
import com.fabriciot.dto.permission.PermissionUserCreateRequest;
import com.fabriciot.dto.permission.PermissionUserUpdateRequest;
import com.fabriciot.entity.SysPermission;
import com.fabriciot.entity.SysRole;
import com.fabriciot.entity.SysRolePermission;
import com.fabriciot.entity.SysUser;
import com.fabriciot.entity.SysUserRole;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.mapper.SysPermissionMapper;
import com.fabriciot.mapper.SysRoleMapper;
import com.fabriciot.mapper.SysRolePermissionMapper;
import com.fabriciot.mapper.SysUserMapper;
import com.fabriciot.mapper.SysUserRoleMapper;
import com.fabriciot.security.SecurityUtils;
import com.fabriciot.service.PermissionService;
import com.fabriciot.vo.permission.PermissionRoleVO;
import com.fabriciot.vo.permission.PermissionTreeVO;
import com.fabriciot.vo.permission.PermissionUserVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final PermissionAssembler permissionAssembler;
    private final PasswordEncoder passwordEncoder;

    public PermissionServiceImpl(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper,
                                 SysPermissionMapper sysPermissionMapper, SysUserRoleMapper sysUserRoleMapper,
                                 SysRolePermissionMapper sysRolePermissionMapper,
                                 PermissionAssembler permissionAssembler, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.permissionAssembler = permissionAssembler;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public IPage<PermissionUserVO> users(PageQuery query) {
        Page<SysUser> userPage = sysUserMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDeleted, 0)
                        .orderByDesc(SysUser::getCreatedAt));
        List<PermissionUserVO> rows = userPage.getRecords().stream()
                .map(this::buildUserVO)
                .toList();
        Page<PermissionUserVO> resultPage = new Page<>(query.getPage(), query.getPageSize(), userPage.getTotal());
        resultPage.setRecords(rows);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionUserVO createUser(PermissionUserCreateRequest request) {
        String username = StringUtils.trimToEmpty(request.getUsername());
        if (sysUserMapper.findAnyByUsername(username) != null) {
            throw new BizException(ErrorCode.BIZ_ERROR, "用户名已存在");
        }

        SysRole role = requireRole(request.getRoleId());
        LocalDateTime now = LocalDateTime.now();

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(normalizeUserStatus(request.getStatus()));
        user.setDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.insert(user);

        replaceUserRole(user.getId(), role.getId());
        return permissionAssembler.toUserVO(user, role.getId(), role.getRoleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionUserVO updateUser(Long userId, PermissionUserUpdateRequest request) {
        SysUser user = requireUser(userId);
        SysRole role = requireRole(request.getRoleId());

        String password = StringUtils.trimToEmpty(request.getPassword());
        if (StringUtils.isNotBlank(password)) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        user.setStatus(normalizeUserStatus(request.getStatus()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        replaceUserRole(userId, role.getId());
        return permissionAssembler.toUserVO(user, role.getId(), role.getRoleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        SysUser user = requireUser(userId);
        if (StringUtils.equals(SecurityUtils.currentUsername(), user.getUsername())) {
            throw new BizException(ErrorCode.BIZ_ERROR, "不能删除当前登录用户");
        }

        user.setDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    @Override
    public IPage<PermissionRoleVO> roles(PageQuery query) {
        Page<SysRole> rolePage = sysRoleMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getDeleted, 0)
                        .orderByDesc(SysRole::getCreatedAt));
        List<PermissionRoleVO> rows = rolePage.getRecords().stream()
                .map(item -> permissionAssembler.toRoleVO(item, defaultCount(sysRoleMapper.countPermissionByRoleId(item.getId()))))
                .toList();
        Page<PermissionRoleVO> resultPage = new Page<>(query.getPage(), query.getPageSize(), rolePage.getTotal());
        resultPage.setRecords(rows);
        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionRoleVO createRole(PermissionRoleCreateRequest request) {
        String roleCode = normalizeRoleCode(request.getRoleCode());
        if (sysRoleMapper.findAnyByRoleCode(roleCode) != null) {
            throw new BizException(ErrorCode.BIZ_ERROR, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        role.setRoleName(StringUtils.trimToEmpty(request.getRoleName()));
        role.setRoleDesc(StringUtils.trimToEmpty(request.getRoleDesc()));
        role.setDeleted(0);
        role.setCreatedAt(LocalDateTime.now());
        sysRoleMapper.insert(role);
        return permissionAssembler.toRoleVO(role, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionRoleVO updateRole(Long roleId, PermissionRoleUpdateRequest request) {
        SysRole role = requireRole(roleId);
        String roleCode = normalizeRoleCode(request.getRoleCode());
        SysRole existing = sysRoleMapper.findAnyByRoleCode(roleCode);
        if (existing != null && !Objects.equals(existing.getId(), roleId)) {
            throw new BizException(ErrorCode.BIZ_ERROR, "角色编码已存在");
        }

        role.setRoleCode(roleCode);
        role.setRoleName(StringUtils.trimToEmpty(request.getRoleName()));
        role.setRoleDesc(StringUtils.trimToEmpty(request.getRoleDesc()));
        sysRoleMapper.updateById(role);

        return permissionAssembler.toRoleVO(role, defaultCount(sysRoleMapper.countPermissionByRoleId(roleId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        SysRole role = requireRole(roleId);
        Long userCount = defaultCount(sysRoleMapper.countUserByRoleId(roleId));
        if (userCount > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR, "该角色已绑定用户，无法删除");
        }

        role.setDeleted(1);
        sysRoleMapper.updateById(role);

        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
    }

    @Override
    public List<PermissionTreeVO> tree() {
        List<SysPermission> permissions = sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getDeleted, 0)
                .orderByAsc(SysPermission::getParentId, SysPermission::getId));
        Map<Long, List<SysPermission>> grouped = permissions.stream()
                .collect(Collectors.groupingBy(item -> item.getParentId() == null ? 0L : item.getParentId()));
        return buildChildren(grouped, 0L);
    }

    @Override
    public List<String> rolePermissions(Long roleId) {
        requireRole(roleId);
        List<SysRolePermission> relations = sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        if (relations.isEmpty()) {
            return List.of();
        }

        Set<Long> permissionIds = relations.stream()
                .map(SysRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return List.of();
        }

        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permissionIds)
                        .eq(SysPermission::getDeleted, 0)
                        .orderByAsc(SysPermission::getId))
                .stream()
                .map(SysPermission::getPermCode)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(Long roleId, List<String> permissionCodes) {
        requireRole(roleId);
        List<String> normalizedCodes = permissionCodes == null
                ? List.of()
                : permissionCodes.stream()
                .map(StringUtils::trimToEmpty)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        List<SysPermission> permissions = normalizedCodes.isEmpty()
                ? List.of()
                : sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getPermCode, normalizedCodes)
                .eq(SysPermission::getDeleted, 0));
        if (permissions.size() != normalizedCodes.size()) {
            Set<String> foundCodes = permissions.stream()
                    .map(SysPermission::getPermCode)
                    .collect(Collectors.toSet());
            List<String> invalidCodes = normalizedCodes.stream()
                    .filter(code -> !foundCodes.contains(code))
                    .toList();
            throw new BizException(ErrorCode.PARAM_ERROR, "存在无效权限编码: " + String.join(",", invalidCodes));
        }

        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        for (SysPermission permission : permissions) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permission.getId());
            sysRolePermissionMapper.insert(relation);
        }
    }

    private PermissionUserVO buildUserVO(SysUser user) {
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        SysRole primaryRole = roles.isEmpty() ? null : roles.get(0);
        return permissionAssembler.toUserVO(
                user,
                primaryRole == null ? null : primaryRole.getId(),
                primaryRole == null ? "" : primaryRole.getRoleName()
        );
    }

    private List<PermissionTreeVO> buildChildren(Map<Long, List<SysPermission>> grouped, Long parentId) {
        List<SysPermission> children = grouped.getOrDefault(parentId, List.of());
        List<PermissionTreeVO> nodes = new ArrayList<>();
        for (SysPermission child : children) {
            PermissionTreeVO node = permissionAssembler.toTreeVO(child);
            List<PermissionTreeVO> childNodes = buildChildren(grouped, child.getId());
            node.setChildren(childNodes.isEmpty() ? null : childNodes);
            nodes.add(node);
        }
        return nodes;
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null || !Objects.equals(role.getDeleted(), 0)) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getDeleted(), 0)) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void replaceUserRole(Long userId, Long roleId) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));

        SysUserRole relation = new SysUserRole();
        relation.setUserId(userId);
        relation.setRoleId(roleId);
        sysUserRoleMapper.insert(relation);
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = StringUtils.upperCase(StringUtils.trimToEmpty(roleCode));
        if (StringUtils.isBlank(normalized)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色编码不能为空");
        }
        return normalized;
    }

    private String normalizeUserStatus(String status) {
        String normalized = StringUtils.trimToEmpty(status);
        try {
            return UserStatus.fromCode(normalized).getCode();
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户状态非法");
        }
    }

    private Long defaultCount(Long value) {
        return value == null ? 0L : value;
    }
}
