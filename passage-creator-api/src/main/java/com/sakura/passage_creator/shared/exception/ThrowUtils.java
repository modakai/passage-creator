package com.sakura.passage_creator.shared.exception;

import com.sakura.passage_creator.shared.common.ErrorCode;

/**
 * 抛异常工具类
 *
 * @author sakura
 * @from sakura
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param runtimeException 运行时异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }

    /**
     * 条件成立则按国际化 key 抛异常。
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param messageKey 消息 key
     * @param args 国际化参数
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String messageKey, Object... args) {
        throwIf(condition, new BusinessException(errorCode, messageKey, args));
    }
}
