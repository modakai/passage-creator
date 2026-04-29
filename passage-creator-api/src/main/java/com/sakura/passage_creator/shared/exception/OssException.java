package com.sakura.passage_creator.shared.exception;

import com.sakura.passage_creator.shared.common.ErrorCode;

/**
 * 处理 OSS 异常
 */
public class OssException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public OssException(int code, String message) {
        super(message);
        this.code = code;
    }

    public OssException(ErrorCode errorCode) {
        super(com.sakura.passage_creator.shared.util.I18nUtils.getMessage(errorCode.getMessageKey(), errorCode.getDefaultMessage()));
        this.code = errorCode.getCode();
    }

    public OssException(ErrorCode errorCode, String message) {
        super(com.sakura.passage_creator.shared.util.I18nUtils.resolveMessage(message));
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
