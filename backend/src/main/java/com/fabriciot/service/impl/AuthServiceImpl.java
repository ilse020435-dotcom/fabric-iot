package com.fabriciot.service.impl;

import com.fabriciot.dto.auth.LoginRequest;
import com.fabriciot.entity.SysRole;
import com.fabriciot.entity.SysUser;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.mapper.SysPermissionMapper;
import com.fabriciot.mapper.SysRoleMapper;
import com.fabriciot.mapper.SysUserMapper;
import com.fabriciot.security.JwtTokenProvider;
import com.fabriciot.service.AuthService;
import com.fabriciot.vo.auth.LoginVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                           SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper,
                           SysPermissionMapper sysPermissionMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public LoginVO login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String token = jwtTokenProvider.createToken(request.getUsername(), authentication.getAuthorities());

            SysUser user = sysUserMapper.findByUsername(request.getUsername());
            String roleText = "设备运营方";
            List<String> permissions = new ArrayList<>();
            if (user != null) {
                List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
                if (!roles.isEmpty() && StringUtils.isNotBlank(roles.get(0).getRoleName())) {
                    roleText = roles.get(0).getRoleName();
                }
                permissions = sysPermissionMapper.selectPermCodesByUserId(user.getId());
            }

            return LoginVO.builder()
                    .token(token)
                    .username(request.getUsername())
                    .role(roleText)
                    .permissions(permissions)
                    .build();
        } catch (AuthenticationException ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
    }

    @Override
    public void logout() {
        // Stateless JWT mode: client side removes token.
    }
}
