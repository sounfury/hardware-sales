package com.hardware.sales.service;

import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.service.dto.DatabaseBackupFile;
import com.hardware.sales.support.SystemProcessExecutor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据库备份服务测试，验证备份和还原都改为通过 Docker 容器内的 MySQL 命令执行。
 */
class DatabaseBackupServiceTest {

    /**
     * 备份时必须调用 Docker 容器内的 mysqldump，并返回可下载的 SQL 内容。
     */
    @Test
    void shouldBackupDatabaseViaDockerMysqldump() throws Exception {
        SystemProcessExecutor executor = mock(SystemProcessExecutor.class);
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("mock backup".getBytes(StandardCharsets.UTF_8)));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor()).thenReturn(0);
        when(executor.start(anyList(), anyBoolean())).thenReturn(process);

        DatabaseBackupService service = new DatabaseBackupService(
                executor,
                "jdbc:mysql://localhost:3306/hardware_sales?useUnicode=true",
                "sounfury",
                "a2133266",
                "docker",
                "mysql",
                true
        );

        DatabaseBackupFile backupFile = service.backupDatabase();

        verify(executor).start(List.of(
                "sudo",
                "docker",
                "exec",
                "mysql",
                "mysqldump",
                "-usounfury",
                "-pa2133266",
                "--single-transaction",
                "--skip-lock-tables",
                "--default-character-set=utf8mb4",
                "hardware_sales"
        ), false);
        assertTrue(backupFile.fileName().startsWith("backup_"));
        assertTrue(backupFile.fileName().endsWith(".sql"));
        assertArrayEquals("mock backup".getBytes(StandardCharsets.UTF_8), backupFile.content());
    }

    /**
     * 还原时必须把上传的 SQL 内容通过标准输入写入 Docker 容器内的 mysql 命令。
     */
    @Test
    void shouldRestoreDatabaseViaDockerMysql() throws Exception {
        SystemProcessExecutor executor = mock(SystemProcessExecutor.class);
        Process process = mock(Process.class);
        ByteArrayOutputStream processInput = new ByteArrayOutputStream();
        when(process.getOutputStream()).thenReturn(processInput);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor()).thenReturn(0);
        when(executor.start(anyList(), anyBoolean())).thenReturn(process);

        DatabaseBackupService service = new DatabaseBackupService(
                executor,
                "jdbc:mysql://localhost:3306/hardware_sales?useUnicode=true",
                "sounfury",
                "a2133266",
                "docker",
                "mysql",
                true
        );

        service.restoreDatabase(new ByteArrayInputStream("restore sql".getBytes(StandardCharsets.UTF_8)));

        verify(executor).start(List.of(
                "sudo",
                "docker",
                "exec",
                "-i",
                "mysql",
                "mysql",
                "-usounfury",
                "-pa2133266",
                "hardware_sales"
        ), true);
        assertEquals("restore sql", processInput.toString(StandardCharsets.UTF_8));
    }

    /**
     * 如果容器内命令执行失败，服务层应抛出明确的业务异常。
     */
    @Test
    void shouldThrowBizExceptionWhenBackupCommandFails() throws Exception {
        SystemProcessExecutor executor = mock(SystemProcessExecutor.class);
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream("docker error".getBytes(StandardCharsets.UTF_8)));
        when(process.waitFor()).thenReturn(1);
        when(executor.start(anyList(), anyBoolean())).thenReturn(process);

        DatabaseBackupService service = new DatabaseBackupService(
                executor,
                "jdbc:mysql://localhost:3306/hardware_sales?useUnicode=true",
                "sounfury",
                "a2133266",
                "docker",
                "mysql",
                true
        );

        BizException exception = assertThrows(BizException.class, service::backupDatabase);

        assertTrue(exception.getMessage().contains("数据库备份失败"));
    }
}
