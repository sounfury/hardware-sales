package com.hardware.sales.service;

import cn.hutool.core.util.StrUtil;
import com.hardware.sales.common.exception.BizException;
import com.hardware.sales.service.dto.DatabaseBackupFile;
import com.hardware.sales.support.SystemProcessExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 数据库备份服务，统一封装备份与还原命令的组装逻辑。
 * 支持直接调用本机 MySQL 客户端，也支持切换为 Docker 容器内执行。
 */
@Service
public class DatabaseBackupService {

    private static final String MODE_LOCAL = "local";
    private static final String MODE_DOCKER = "docker";

    private final SystemProcessExecutor processExecutor;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final String backupMode;
    private final String dockerContainerName;
    private final boolean useSudo;

    /**
     * 根据当前数据源配置和备份执行模式初始化数据库备份服务。
     */
    public DatabaseBackupService(SystemProcessExecutor processExecutor,
                                 @Value("${spring.datasource.url}") String dbUrl,
                                 @Value("${spring.datasource.username}") String dbUsername,
                                 @Value("${spring.datasource.password}") String dbPassword,
                                 @Value("${hardware.database.backup.mode:local}") String backupMode,
                                 @Value("${hardware.database.docker.container:mysql}") String dockerContainerName,
                                 @Value("${hardware.database.docker.use-sudo:false}") boolean useSudo) {
        this.processExecutor = processExecutor;
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.backupMode = StrUtil.blankToDefault(backupMode, MODE_LOCAL).toLowerCase(Locale.ROOT);
        this.dockerContainerName = dockerContainerName;
        this.useSudo = useSudo;
    }

    /**
     * 生成一份数据库 SQL 备份文件，并返回供控制器下载的字节内容。
     */
    public DatabaseBackupFile backupDatabase() {
        String fileName = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        Process process = null;
        try {
            process = processExecutor.start(buildBackupCommand(), false);
            byte[] content;
            try (InputStream inputStream = process.getInputStream()) {
                content = inputStream.readAllBytes();
            }
            String error = readStream(process.getErrorStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BizException("数据库备份失败：" + buildErrorMessage(exitCode, error));
            }
            return new DatabaseBackupFile(fileName, content);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("数据库备份失败：" + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 将上传的 SQL 备份文件还原回当前数据库。
     */
    public void restoreDatabase(InputStream backupInputStream) {
        Process process = null;
        try {
            process = processExecutor.start(buildRestoreCommand(), true);
            try (OutputStream outputStream = process.getOutputStream();
                 InputStream inputStream = backupInputStream) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }
            String commandOutput = readStream(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BizException("数据库还原失败：" + buildErrorMessage(exitCode, commandOutput));
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("数据库还原失败：" + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private List<String> buildBackupCommand() {
        List<String> command = buildCommandPrefix(false);
        command.add("mysqldump");
        appendConnectionArgs(command);
        command.add("--single-transaction");
        command.add("--skip-lock-tables");
        command.add("--default-character-set=utf8mb4");
        command.add(extractConnectionInfo().databaseName());
        return command;
    }

    private List<String> buildRestoreCommand() {
        List<String> command = buildCommandPrefix(true);
        command.add("mysql");
        appendConnectionArgs(command);
        command.add(extractConnectionInfo().databaseName());
        return command;
    }

    private List<String> buildCommandPrefix(boolean interactive) {
        if (MODE_LOCAL.equals(backupMode)) {
            return new ArrayList<>();
        }
        if (MODE_DOCKER.equals(backupMode)) {
            return buildDockerPrefix(interactive);
        }
        throw new BizException("数据库备份模式配置错误，仅支持 local 或 docker");
    }

    private List<String> buildDockerPrefix(boolean interactive) {
        List<String> command = new ArrayList<>();
        if (useSudo) {
            command.add("sudo");
        }
        command.add("docker");
        command.add("exec");
        if (interactive) {
            command.add("-i");
        }
        command.add(dockerContainerName);
        return command;
    }

    private void appendConnectionArgs(List<String> command) {
        if (MODE_LOCAL.equals(backupMode)) {
            DatabaseConnectionInfo connectionInfo = extractConnectionInfo();
            command.add("-h" + connectionInfo.host());
            command.add("-P" + connectionInfo.port());
        }
        command.add("-u" + dbUsername);
        if (StrUtil.isNotBlank(dbPassword)) {
            command.add("-p" + dbPassword);
        }
    }

    /**
     * 从 JDBC URL 中提取主机、端口和数据库名，避免本机模式下把连接信息写死。
     */
    private DatabaseConnectionInfo extractConnectionInfo() {
        try {
            URI uri = URI.create(StrUtil.removePrefix(dbUrl, "jdbc:"));
            String databaseName = StrUtil.removePrefix(uri.getPath(), "/");
            if (StrUtil.isBlank(databaseName)) {
                throw new BizException("数据库连接配置错误，未找到数据库名");
            }
            String host = StrUtil.blankToDefault(uri.getHost(), "localhost");
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            return new DatabaseConnectionInfo(host, port, databaseName);
        } catch (IllegalArgumentException e) {
            throw new BizException("数据库连接配置错误，无法解析数据源地址");
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    private String buildErrorMessage(int exitCode, String output) {
        if (StrUtil.isNotBlank(output)) {
            return output;
        }
        return "退出码：" + exitCode;
    }

    private record DatabaseConnectionInfo(String host, int port, String databaseName) {
    }
}
