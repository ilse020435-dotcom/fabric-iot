package com.fabriciot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fabriciot.common.enums.UserStatus;
import com.fabriciot.entity.SysPermission;
import com.fabriciot.entity.SysRole;
import com.fabriciot.entity.SysRolePermission;
import com.fabriciot.entity.SysUser;
import com.fabriciot.entity.SysUserRole;
import com.fabriciot.mapper.SysPermissionMapper;
import com.fabriciot.mapper.SysRoleMapper;
import com.fabriciot.mapper.SysRolePermissionMapper;
import com.fabriciot.mapper.SysUserMapper;
import com.fabriciot.mapper.SysUserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class BootstrapDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    @Value("${app.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final PasswordEncoder passwordEncoder;

    public BootstrapDataInitializer(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper,
                                    SysPermissionMapper sysPermissionMapper, SysUserRoleMapper sysUserRoleMapper,
                                    SysRolePermissionMapper sysRolePermissionMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        Map<String, Long> permissionIdMap = ensurePermissions();

        SysRole adminRole = ensureRole("ADMIN", "系统管理员", "拥有全部配置、运维与审计权限");
        SysRole operatorRole = ensureRole("OPERATOR", "设备运营方", "负责设备激活、冻结、注销和监控");
        SysRole vendorRole = ensureRole("VENDOR", "设备厂商", "负责设备注册与元数据维护");
        SysRole regulatorRole = ensureRole("REGULATOR", "监管机构", "负责审计与链上记录监管");

        replaceRolePermissions(adminRole.getId(), permissionIdMap.values().stream().toList());
        replaceRolePermissions(operatorRole.getId(), toPermissionIds(permissionIdMap,
                "dashboard:view", "device", "device:list", "device:update", "device:freeze", "device:revoke",
                "monitor:view", "audit:view", "blockchain:view"));
        replaceRolePermissions(vendorRole.getId(), toPermissionIds(permissionIdMap,
                "dashboard:view", "device", "device:list", "device:create", "device:update",
                "monitor:view", "blockchain:view"));
        replaceRolePermissions(regulatorRole.getId(), toPermissionIds(permissionIdMap,
                "dashboard:view", "device", "device:list", "monitor:view", "audit:view", "blockchain:view"));

        replaceUserRole(ensureUser("admin"), adminRole.getId());
        replaceUserRole(ensureUser("operator"), operatorRole.getId());
        replaceUserRole(ensureUser("vendor"), vendorRole.getId());
        replaceUserRole(ensureUser("regulator"), regulatorRole.getId());

        log.info("Bootstrap data initialized");
    }

    private Map<String, Long> ensurePermissions() {
        Map<String, PermissionSeed> seeds = new LinkedHashMap<>();
        seeds.put("dashboard:view", new PermissionSeed("首页查看", "MENU", null));
        seeds.put("device", new PermissionSeed("设备管理", "MENU", null));
        seeds.put("device:list", new PermissionSeed("设备列表查看", "MENU", "device"));
        seeds.put("device:create", new PermissionSeed("设备注册", "BUTTON", "device"));
        seeds.put("device:update", new PermissionSeed("设备编辑", "BUTTON", "device"));
        seeds.put("device:freeze", new PermissionSeed("设备冻结", "BUTTON", "device"));
        seeds.put("device:revoke", new PermissionSeed("设备注销", "BUTTON", "device"));
        seeds.put("monitor:view", new PermissionSeed("状态监控", "MENU", null));
        seeds.put("audit:view", new PermissionSeed("审计日志查看", "MENU", null));
        seeds.put("blockchain:view", new PermissionSeed("区块链记录查看", "MENU", null));
        seeds.put("fabric:diagnose", new PermissionSeed("Fabric诊断", "MENU", null));
        seeds.put("permission", new PermissionSeed("权限管理", "MENU", null));
        seeds.put("user:manage", new PermissionSeed("用户管理", "BUTTON", "permission"));
        seeds.put("role:manage", new PermissionSeed("角色管理", "BUTTON", "permission"));

        seeds.put("device:import", new PermissionSeed("设备Excel导入", "BUTTON", "device"));

        Map<String, Long> idMap = new LinkedHashMap<>();
        for (Map.Entry<String, PermissionSeed> entry : seeds.entrySet()) {
            String code = entry.getKey();
            PermissionSeed seed = entry.getValue();
            Long parentId = seed.parentCode == null ? 0L : idMap.getOrDefault(seed.parentCode, 0L);

            SysPermission permission = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getPermCode, code)
                    .eq(SysPermission::getDeleted, 0)
                    .last("LIMIT 1"));
            if (permission == null) {
                permission = new SysPermission();
                permission.setPermCode(code);
            }
            permission.setPermName(seed.permName);
            permission.setType(seed.type);
            permission.setParentId(parentId);
            permission.setDeleted(0);

            if (permission.getId() == null) {
                sysPermissionMapper.insert(permission);
            } else {
                sysPermissionMapper.updateById(permission);
            }
            idMap.put(code, permission.getId());
        }
        return idMap;
    }

    private SysRole ensureRole(String roleCode, String roleName, String roleDesc) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .eq(SysRole::getDeleted, 0)
                .last("LIMIT 1"));
        if (role == null) {
            role = new SysRole();
            role.setRoleCode(roleCode);
            role.setCreatedAt(LocalDateTime.now());
        }
        role.setRoleName(roleName);
        role.setRoleDesc(roleDesc);
        role.setDeleted(0);

        if (role.getId() == null) {
            sysRoleMapper.insert(role);
        } else {
            sysRoleMapper.updateById(role);
        }
        return role;
    }

    private SysUser ensureUser(String username) {
        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(UserStatus.ENABLED.getCode());
        user.setDeleted(0);
        user.setUpdatedAt(LocalDateTime.now());

        if (user.getId() == null) {
            sysUserMapper.insert(user);
        } else {
            sysUserMapper.updateById(user);
        }
        return user;
    }

    private List<Long> toPermissionIds(Map<String, Long> permissionIdMap, String... permCodes) {
        return List.of(permCodes).stream()
                .map(permissionIdMap::get)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .toList();
    }

    private void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        sysRolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        for (Long permissionId : permissionIds.stream().distinct().toList()) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            sysRolePermissionMapper.insert(relation);
        }
    }

    private void replaceUserRole(SysUser user, Long roleId) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, user.getId()));
        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(roleId);
        sysUserRoleMapper.insert(relation);
    }

    private record PermissionSeed(String permName, String type, String parentCode) {
    }
}
