package com.hardware.sales.support;

import java.io.IOException;
import java.util.List;

/**
 * 系统进程执行器，统一封装对外部命令的启动能力，便于测试时替换。
 */
public interface SystemProcessExecutor {

    /**
     * 启动一个外部命令进程。
     */
    Process start(List<String> command, boolean redirectErrorStream) throws IOException;
}
