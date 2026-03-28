package com.fabriciot.service;

import com.fabriciot.dto.auth.LoginRequest;
import com.fabriciot.vo.auth.LoginVO;

public interface AuthService {

    LoginVO login(LoginRequest request);

    void logout();
}

