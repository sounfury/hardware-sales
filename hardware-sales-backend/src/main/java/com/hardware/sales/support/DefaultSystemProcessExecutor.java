package com.hardware.sales.support;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 默认系统进程执行器，直接基于 ProcessBuilder 启动命令。
 */
@Component
public class DefaultSystemProcessExecutor implements SystemProcessExecutor {

    /**
     * 按传入命令启动外部进程，并允许调用方决定是否合并错误流。
     */
    @Override
    public Process start(List<String> command, boolean redirectErrorStream) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(redirectErrorStream);
        return processBuilder.start();
    }
}
