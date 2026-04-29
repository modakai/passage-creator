package com.sakura.passage_creator.shared.common;

/**
 * 返回工具类
 *
 * @author sakura
 * @from sakura
 */
public class ResultUtils {

    /**
     * 成功返回
     *
     * @param data 数据
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, ErrorCode.SUCCESS.getDefaultMessage());
    }

    /**
     * 失败返回
     *
     * @param errorCode 错误码
     * @return 响应结果
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败返回
     *
     * @param code 错误码
     * @param message 错误信息
     * @return 响应结果
     */
    public static BaseResponse error(int code, String message) {
        return new BaseResponse(code, null, message);
    }

    /**
     * 失败返回
     *
     * @param errorCode 错误码
     * @param message 错误信息
     * @return 响应结果
     */
    public static BaseResponse error(ErrorCode errorCode, String message) {
        return new BaseResponse(errorCode.getCode(), null, com.sakura.passage_creator.shared.util.I18nUtils.resolveMessage(message));
    }

    /**
     * 失败返回，支持消息 key 与格式化参数。
     *
     * @param errorCode 错误码
     * @param messageKey 消息 key
     * @param args 格式化参数
     * @return 响应结果
     */
    public static BaseResponse error(ErrorCode errorCode, String messageKey, Object... args) {
        return new BaseResponse(errorCode.getCode(), null,
                com.sakura.passage_creator.shared.util.I18nUtils.getMessage(messageKey, errorCode.getDefaultMessage(), args));
    }
}
