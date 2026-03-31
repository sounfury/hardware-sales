package com.hardware.sales.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.service.SysUserService;
import com.hardware.sales.service.dto.AuthLoginRequest;
import com.hardware.sales.service.dto.MiniappRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器，提供后台登录、小程序登录注册、登出、获取当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 后台管理员登录，后端强制只允许 ADMIN 角色进入。
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody AuthLoginRequest loginRequest) {
        SysUser user = authenticate(loginRequest);
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException("当前后台仅允许管理员登录");
        }
        StpUtil.login(user.getId());
        return Result.ok(buildLoginResult(user));
    }

    /**
     * 小程序登录，允许已审核供应商和待申请用户进入小程序。
     */
    @PostMapping("/miniapp/login")
    public Result<?> miniappLogin(@RequestBody AuthLoginRequest loginRequest) {
        SysUser user = authenticate(loginRequest);
        if (StrUtil.isNotBlank(user.getRole()) && !"SUPPLIER".equals(user.getRole())) {
            throw new BizException("当前账号不可进入供应商小程序");
        }
        StpUtil.login(user.getId());
        return Result.ok(buildLoginResult(user));
    }

    /**
     * 小程序注册，创建空角色账号并直接建立登录态。
     */
    @PostMapping("/miniapp/register")
    public Result<?> miniappRegister(@RequestBody MiniappRegisterRequest registerRequest) {
        validateRegisterRequest(registerRequest);

        SysUser user = new SysUser();
        user.setUsername(StrUtil.trim(registerRequest.getUsername()));
        user.setPassword(registerRequest.getPassword());
        user.setNickname(StrUtil.blankToDefault(StrUtil.trim(registerRequest.getNickname()), user.getUsername()));
        user.setPhone(StrUtil.trim(registerRequest.getPhone()));
        user.setRole("");
        user.setStatus(1);
        sysUserService.createUser(user);

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

    /**
     * 校验账号密码并返回用户实体。
     */
    private SysUser authenticate(AuthLoginRequest loginRequest) {
        validateLoginRequest(loginRequest);

        SysUser user = sysUserService.lambdaQuery()
                .eq(SysUser::getUsername, StrUtil.trim(loginRequest.getUsername()))
                .one();
        if (user == null || !BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 校验登录参数。
     */
    private void validateLoginRequest(AuthLoginRequest loginRequest) {
        if (loginRequest == null
                || StrUtil.isBlank(loginRequest.getUsername())
                || StrUtil.isBlank(loginRequest.getPassword())) {
            throw new BizException("用户名和密码不能为空");
        }
    }

    /**
     * 校验小程序注册参数。
     */
    private void validateRegisterRequest(MiniappRegisterRequest registerRequest) {
        if (registerRequest == null
                || StrUtil.isBlank(registerRequest.getUsername())
                || StrUtil.isBlank(registerRequest.getPassword())) {
            throw new BizException("用户名和密码不能为空");
        }
    }

    /**
     * 统一构造登录返回数据。
     */
    private Map<String, Object> buildLoginResult(SysUser user) {
        return Map.of(
                "token", StpUtil.getTokenValue(),
                "user", user
        );
    }
}
