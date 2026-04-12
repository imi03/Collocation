package com.gba.advisor.module.auth.controller;
import com.gba.advisor.common.result.Result;
import com.gba.advisor.module.auth.dto.LoginRequest;
import com.gba.advisor.module.auth.dto.TokenResponse;
import com.gba.advisor.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name="认证", description="登录/注册/Token刷新")
@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary="用户登录")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest req) { return Result.ok(authService.login(req)); }

    @Operation(summary="用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody LoginRequest req) { authService.register(req); return Result.ok(); }

    @Operation(summary="刷新AccessToken")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@RequestParam String refreshToken) { return Result.ok(authService.refreshToken(refreshToken)); }
}
