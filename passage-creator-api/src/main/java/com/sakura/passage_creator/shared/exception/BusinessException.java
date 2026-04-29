package com.sakura.passage_creator.shared.exception;

import com.sakura.passage_creator.shared.common.ErrorCode;

/**
 * 鑷畾涔夊紓甯哥被
 *
 * @author Sakura
 */
public class BusinessException extends RuntimeException {

    /**
     * 閿欒鐮?     */
    private final int code;

    /**
     * 消息 key 或原始文案。
     */
    private final String messageKey;

    /**
     * 国际化参数。
     */
    private final transient Object[] args;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.messageKey = message;
        this.args = new Object[0];
    }

    public BusinessException(ErrorCode errorCode) {
        super(com.sakura.passage_creator.shared.util.I18nUtils.getMessage(errorCode.getMessageKey(), errorCode.getDefaultMessage()));
        this.code = errorCode.getCode();
        this.messageKey = errorCode.getMessageKey();
        this.args = new Object[0];
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(com.sakura.passage_creator.shared.util.I18nUtils.resolveMessage(message));
        this.code = errorCode.getCode();
        this.messageKey = message;
        this.args = new Object[0];
    }

    public BusinessException(ErrorCode errorCode, String messageKey, Object... args) {
        super(com.sakura.passage_creator.shared.util.I18nUtils.getMessage(messageKey, errorCode.getDefaultMessage(), args));
        this.code = errorCode.getCode();
        this.messageKey = messageKey;
        this.args = args;
    }

    public int getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}




