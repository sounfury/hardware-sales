package com.hardware.sales.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器，提供 Web 端账号密码登录、登出、获取当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 账号密码登录，返回 token 和用户信息
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginForm) {
        String username = loginForm.get("username");
        String password = loginForm.get("password");

        SysUser user = sysUserService.lambdaQuery()
                .eq(SysUser::getUsername, username)
                .one();
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException("当前后台仅允许管理员登录");
        }

        StpUtil.login(user.getId());

        user.setPassword(null);
        return Result.ok(Map.of(
                "token", StpUtil.getTokenValue(),
                "user", user
        ));
    }

    /**
     * 登出当前用户
     */
    @PostMapping("/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<SysUser> info() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }
}
