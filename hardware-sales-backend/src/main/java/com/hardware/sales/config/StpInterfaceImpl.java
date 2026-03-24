package com.hardware.sales.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.StrUtil;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * SA-Token 角色数据提供器，供 @SaCheckRole 从数据库读取当前用户角色。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserService sysUserService;

    /**
     * 当前项目暂未启用细粒度权限点校验，统一返回空集合。
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 返回当前登录用户角色列表，供角色注解进行权限判断。
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = sysUserService.getById(Long.parseLong(String.valueOf(loginId)));
        if (user == null || StrUtil.isBlank(user.getRole())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRole());
    }
}
