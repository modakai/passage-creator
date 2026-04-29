package com.sakura.passage_creator.shared.exception;

import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author sakura
 * @from sakura
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, extractBindingErrorMessage(e.getBindingResult().getFieldError()));
    }

    /**
     * 处理表单或对象绑定参数校验异常。
     *
     * @param e 绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    public BaseResponse<?> bindExceptionHandler(BindException e) {
        log.warn("BindException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, extractBindingErrorMessage(e.getBindingResult().getFieldError()));
    }

    /**
     * 处理方法级参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> constraintViolationExceptionHandler(ConstraintViolationException e) {
        log.warn("ConstraintViolationException", e);
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(constraintViolation -> constraintViolation.getMessage())
                .orElse(com.sakura.passage_creator.shared.util.I18nUtils.getMessage(
                        ErrorCode.PARAMS_ERROR.getMessageKey(),
                        ErrorCode.PARAMS_ERROR.getDefaultMessage()));
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 提取字段校验失败提示。
     *
     * @param fieldError 字段错误
     * @return 错误提示
     */
    private String extractBindingErrorMessage(FieldError fieldError) {
        if (fieldError == null) {
            return com.sakura.passage_creator.shared.util.I18nUtils.getMessage(
                    ErrorCode.PARAMS_ERROR.getMessageKey(),
                    ErrorCode.PARAMS_ERROR.getDefaultMessage());
        }
        return fieldError.getDefaultMessage();
    }
}
