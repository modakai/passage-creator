package com.sakura.passage_creator.infrastructure.aop;

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

import java.util.UUID;

/**
 * 请求日志切面。
 *
 * 作者：Sakura
 */
@Aspect
@Component
@Slf4j
public class LogInterceptor {

    /**
     * 记录控制器请求日志。
     *
     * @param point 切点
     * @return 原方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("execution(* com.sakura.passage_creator..controller..*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String requestId = UUID.randomUUID().toString();
        String url = request.getRequestURI();
        String reqParam = "[" + StringUtils.join(point.getArgs(), ", ") + "]";

        log.info("request start, id: {}, path: {}, ip: {}, params: {}",
                requestId, url, request.getRemoteHost(), reqParam);

        Object result = point.proceed();

        stopWatch.stop();
        log.info("request end, id: {}, cost: {}ms", requestId, stopWatch.getTotalTimeMillis());
        return result;
    }
}
