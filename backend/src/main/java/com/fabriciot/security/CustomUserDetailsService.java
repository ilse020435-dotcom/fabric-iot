package com.fabriciot.security;

import com.fabriciot.common.enums.UserStatus;
import com.fabriciot.entity.SysRole;
import com.fabriciot.entity.SysUser;
import com.fabriciot.mapper.SysPermissionMapper;
import com.fabriciot.mapper.SysRoleMapper;
import com.fabriciot.mapper.SysUserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public CustomUserDetailsService(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper, SysPermissionMapper sysPermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (UserStatus.DISABLED.getCode().equalsIgnoreCase(user.getStatus())) {
            throw new DisabledException("用户已禁用");
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<SysRole> roleList = sysRoleMapper.selectByUserId(user.getId());
        roleList.stream()
                .map(SysRole::getRoleCode)
                .map(roleCode -> "ROLE_" + roleCode)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        sysPermissionMapper.selectPermCodesByUserId(user.getId())
                .stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
