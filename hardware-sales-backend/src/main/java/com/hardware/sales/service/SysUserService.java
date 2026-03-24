package com.hardware.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hardware.sales.entity.SysUser;

/**
 * 系统用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /** 分页查询用户，支持按用户名、角色筛选 */
    IPage<SysUser> pageQuery(Integer pageNum, Integer pageSize, String username, String role);

    /** 创建用户，密码 BCrypt 加密后存储 */
    void createUser(SysUser user);

    /** 修改用户信息（不允许修改密码） */
    void updateUser(SysUser user);

    /** 重置用户密码 */
    void resetPassword(Long id, String newPassword);
}
