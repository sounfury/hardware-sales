package com.hardware.sales.service.dto;

/**
 * 数据库备份文件载体，封装下载文件名和 SQL 文件内容。
 */
public record DatabaseBackupFile(String fileName, byte[] content) {
}
