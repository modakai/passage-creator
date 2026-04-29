package com.sakura.passage_creator.shared.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.sakura.passage_creator.shared.util.I18nUtils;

/**
 * 通用返回类
 *
 * @param <T> 数据类型
 * @author sakura
 * @from sakura
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    /**
     * 扩展数据，默认返回空对象，保持前端结构稳定。
     */
    private Map<String, Object> extra;

    private String message;

    /**
     * 是否成功，便于前端快速判断，但业务仍以 code 为准。
     */
    private boolean success;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.extra = Collections.emptyMap();
        this.message = message;
        this.success = ErrorCode.SUCCESS.getCode() == code;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, I18nUtils.getMessage(errorCode.getMessageKey(), errorCode.getDefaultMessage()));
    }
}
