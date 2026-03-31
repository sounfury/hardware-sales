package com.hardware.sales.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.mapper.SysUserMapper;
import com.hardware.sales.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 系统用户服务实现，处理用户创建（密码加密）、更新、密码重置
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    @Override
    public IPage<SysUser> pageQuery(Integer pageNum, Integer pageSize, String username, String role) {
        return lambdaQuery()
                .like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                .eq(StrUtil.isNotBlank(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    @Override
    public void createUser(SysUser user) {
        // 检查用户名是否已存在
        long count = lambdaQuery().eq(SysUser::getUsername, user.getUsername()).count();
        if (count > 0) {
            throw new BizException("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole("");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        save(user);
    }

    @Override
    public void updateUser(SysUser user) {
        // 不允许通过此接口修改密码
        user.setPassword(null);
        updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPassword(BCrypt.hashpw(newPassword));
        updateById(user);
    }
}
