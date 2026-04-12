package com.gba.advisor.module.auth.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gba.advisor.common.exception.BusinessException;
import com.gba.advisor.common.result.ResultCode;
import com.gba.advisor.common.utils.JwtUtils;
import com.gba.advisor.module.auth.dto.LoginRequest;
import com.gba.advisor.module.auth.dto.TokenResponse;
import com.gba.advisor.module.auth.entity.User;
import com.gba.advisor.module.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j @Service @RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public TokenResponse login(LoginRequest req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, req.getUsername())
                .select(User::getId, User::getUsername, User::getPassword, User::getNickname));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        log.info("用户登录成功: {}", user.getUsername());
        return new TokenResponse(jwtUtils.generateAccessToken(user.getId(), user.getUsername()),
                jwtUtils.generateRefreshToken(user.getId(), user.getUsername()),
                user.getId(), user.getUsername(), user.getNickname());
    }

    public void register(LoginRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) > 0)
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getUsername());
        u.setRole("USER");
        userMapper.insert(u);
        log.info("用户注册成功: {}", u.getUsername());
    }

    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validate(refreshToken)) throw new BusinessException(ResultCode.TOKEN_INVALID);
        Long userId = jwtUtils.getUserId(refreshToken);
        String username = jwtUtils.getUsername(refreshToken);
        return new TokenResponse(jwtUtils.generateAccessToken(userId, username),
                jwtUtils.generateRefreshToken(userId, username), userId, username, null);
    }
}
