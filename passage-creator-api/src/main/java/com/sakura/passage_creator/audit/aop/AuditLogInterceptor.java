package com.sakura.passage_creator.audit.aop;

import com.sakura.passage_creator.audit.model.dto.AuditLogCreateRequest;
import com.sakura.passage_creator.audit.service.AuditLogService;
import com.sakura.passage_creator.shared.annotation.AuditLogRecord;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.util.NetUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 管理员操作审计切面。
 *
 * @author Sakura
 */
@Aspect
@Component
@Slf4j
public class AuditLogInterceptor {

    /**
     * 审计日志服务。
     */
    @Resource
    private AuditLogService auditLogService;

    /**
     * 拦截带审计注解的方法并记录执行结果。
     *
     * @param joinPoint 切点
     * @param auditLogRecord 审计注解
     * @return 原方法结果
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(auditLogRecord)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuditLogRecord auditLogRecord) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Throwable throwable = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            stopWatch.stop();
            try {
                AuditLogCreateRequest request = buildAuditRequest(joinPoint, auditLogRecord, result, throwable);
                auditLogService.submitOperationLog(request, throwable == null && isSuccessResponse(result),
                        throwable, stopWatch.getTotalTimeMillis());
            } catch (Exception auditException) {
                log.error("record audit operation log failed", auditException);
            }
        }
    }

    /**
     * 构造操作审计请求。
     */
    private AuditLogCreateRequest buildAuditRequest(ProceedingJoinPoint joinPoint, AuditLogRecord record,
            Object result, Throwable throwable) {
        HttpServletRequest servletRequest = currentRequest();
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        AuditLogCreateRequest request = new AuditLogCreateRequest();
        request.setUserId(loginUser == null ? null : loginUser.userId());
        request.setAccountIdentifier(loginUser == null ? null : loginUser.userAccount());
        request.setRequestPath(servletRequest == null ? null : servletRequest.getRequestURI());
        request.setHttpMethod(servletRequest == null ? null : servletRequest.getMethod());
        request.setIpAddress(servletRequest == null ? null : NetUtils.getIpAddress(servletRequest));
        request.setClientInfo(servletRequest == null ? null : servletRequest.getHeader("User-Agent"));
        request.setTraceId(servletRequest == null ? UUID.randomUUID().toString() : getTraceId(servletRequest));
        request.setOperationDescription(record.description());
        request.setBusinessModule(record.module());
        request.setOperationType(record.operationType().getValue());
        request.setStatusCode(resolveStatusCode(result, throwable));
        if (record.recordRequest()) {
            request.setRequestSummary(buildRequestSummary(joinPoint.getArgs()));
        }
        if (record.recordResponse()) {
            request.setResponseSummary(String.valueOf(result));
        }
        return request;
    }

    /**
     * 获取当前 HTTP 请求。
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    /**
     * 解析追踪 ID，优先沿用前端或网关注入的请求头。
     */
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        return StringUtils.defaultIfBlank(traceId, UUID.randomUUID().toString());
    }

    /**
     * 判断 BaseResponse 是否代表业务成功。
     */
    private boolean isSuccessResponse(Object result) {
        if (result instanceof BaseResponse<?> response) {
            return response.getCode() == 0;
        }
        return true;
    }

    /**
     * 解析状态码。
     */
    private Integer resolveStatusCode(Object result, Throwable throwable) {
        if (throwable != null) {
            return 50000;
        }
        if (result instanceof BaseResponse<?> response) {
            return response.getCode();
        }
        return 0;
    }

    /**
     * 构造参数摘要，过滤掉 HTTP 原生对象。
     */
    private String buildRequestSummary(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }
}
