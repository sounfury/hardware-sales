package com.hardware.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.common.result.Result;
import com.hardware.sales.service.DatabaseBackupService;
import com.hardware.sales.entity.SysUser;
import com.hardware.sales.service.SysUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * 系统管理控制器，提供用户管理（增删改查、重置密码）和数据库备份还原
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class SystemController {

    private final SysUserService sysUserService;
    private final DatabaseBackupService databaseBackupService;

    // ==================== 用户管理 ====================

    /** 分页查询用户，支持按用户名、角色筛选 */
    @GetMapping("/user/page")
    public Result<IPage<SysUser>> userPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        return Result.ok(sysUserService.pageQuery(pageNum, pageSize, username, role));
    }

    /** 根据 ID 查询用户（密码字段置空） */
    @GetMapping("/user/{id}")
    public Result<SysUser> getUser(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }

    /** 创建用户 */
    @PostMapping("/user")
    public Result<?> createUser(@RequestBody SysUser user) {
        sysUserService.createUser(user);
        return Result.ok();
    }

    /** 修改用户信息（不允许修改密码） */
    @PutMapping("/user")
    public Result<?> updateUser(@RequestBody SysUser user) {
        sysUserService.updateUser(user);
        return Result.ok();
    }

    /** 删除用户 */
    @DeleteMapping("/user/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.ok();
    }

    /** 重置用户密码 */
    @PutMapping("/user/reset-password/{id}")
    public Result<?> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        sysUserService.resetPassword(id, newPassword);
        return Result.ok();
    }

    // ==================== 数据库备份还原 ====================

    /** 导出数据库备份文件，实际执行方式由后端当前的数据库备份模式决定。 */
    @GetMapping("/db/backup")
    public void backup(HttpServletResponse response) {
        try {
            var backupFile = databaseBackupService.backupDatabase();

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + backupFile.fileName());
            try (OutputStream os = response.getOutputStream()) {
                os.write(backupFile.content());
                os.flush();
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("数据库备份失败：" + e.getMessage());
        }
    }

    /** 上传 .sql 备份文件还原数据库，实际执行方式由后端当前的数据库备份模式决定。 */
    @PostMapping("/db/restore")
    public Result<?> restore(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException("请选择备份文件");
        }

        try {
            databaseBackupService.restoreDatabase(file.getInputStream());
            return Result.ok();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("数据库还原失败：" + e.getMessage());
        }
    }
}
