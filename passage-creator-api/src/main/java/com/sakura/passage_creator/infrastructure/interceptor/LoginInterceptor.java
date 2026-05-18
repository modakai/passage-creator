package com.sakura.passage_creator.infrastructure.interceptor;

import com.sakura.passage_creator.infrastructure.auth.LoginUserProvider;
import com.sakura.passage_creator.infrastructure.auth.OnlineSessionTracker;
import com.sakura.passage_creator.shared.annotation.NoLoginRequired;
import com.sakura.passage_creator.infrastructure.auth.TokenManager;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器，负责解析 token 并写入当前请求用户上下文。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor, AsyncHandlerInterceptor {

    /**
     * Token 管理器。
     */
    private final TokenManager tokenManager;

    /**
     * 登录用户加载端口。
     */
    private final LoginUserProvider loginUserProvider;

    /**
     * 在线用户服务。
     */
    private final OnlineSessionTracker onlineSessionTracker;

    public LoginInterceptor(TokenManager tokenManager, LoginUserProvider loginUserProvider,
            OnlineSessionTracker onlineSessionTracker) {
        this.tokenManager = tokenManager;
        this.loginUserProvider = loginUserProvider;
        this.onlineSessionTracker = onlineSessionTracker;
    }

    /**
     * 请求进入 Controller 前完成登录校验。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @return 是否放行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (isNoLoginRequired(handlerMethod)) {
            return true;
        }
        String token = tokenManager.resolveToken(request);
        Long userId = tokenManager.getUserId(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        LoginUserInfo loginUserInfo = loginUserProvider.loadLoginUser(userId);
        if (loginUserInfo == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        LoginUserContext.setLoginUser(loginUserInfo);
        // userId 已在本拦截器中解析完成，直接传递可避免在线会话刷新重复读取 token 映射。
        onlineSessionTracker.refreshLastAccess(userId);
        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     * @param ex 异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    /**
     * 异步请求启动后当前 Servlet 线程会被复用，必须立即清理登录上下文。
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param handler 处理器
     */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
            Object handler) {
        LoginUserContext.clear();
    }

    /**
     * 判断接口或 Controller 是否允许未登录访问。
     *
     * @param handlerMethod Controller 方法
     * @return 是否放行
     */
    private boolean isNoLoginRequired(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(NoLoginRequired.class)
                || handlerMethod.getBeanType().isAnnotationPresent(NoLoginRequired.class);
    }
}
