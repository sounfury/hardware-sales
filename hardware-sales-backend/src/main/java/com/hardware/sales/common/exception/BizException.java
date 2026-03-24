package com.hardware.sales.common.exception;

import lombok.Getter;

/**
 * 业务异常，抛出后由全局异常处理器捕获并转为统一响应
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
