package com.sakura.passage_creator.observability.support;

import com.sakura.passage_creator.observability.config.ObservabilityProperties;
import com.sakura.passage_creator.observability.model.dto.RequestObservationCommand;
import com.sakura.passage_creator.observability.service.ObservabilityEventService;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.util.NetUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

/**
 * 请求观测过滤器，采集接口耗时、状态码、用户和异常摘要。
 *
 * @author Sakura
 */
@Component
public class ObservabilityRequestMonitorFilter extends OncePerRequestFilter {

    /**
     * 事件服务。
     */
    private final ObservabilityEventService eventService;

    /**
     * 可观测性配置。
     */
    private final ObservabilityProperties properties;

    public ObservabilityRequestMonitorFilter(ObservabilityEventService eventService, ObservabilityProperties properties) {
        this.eventService = eventService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !properties.isRequestMonitorEnabled()
                || uri.contains("/actuator")
                || uri.contains("/admin/observability/status");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        Throwable throwable = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            recordRequest(request, response, throwable, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 记录请求观测事实。
     */
    private void recordRequest(HttpServletRequest request, HttpServletResponse response, Throwable throwable,
            long durationMillis) {
        LoginUserInfo user = LoginUserContext.getLoginUser();
        RequestObservationCommand command = new RequestObservationCommand();
        command.setRequestPath(request.getRequestURI());
        command.setHttpMethod(request.getMethod());
        command.setStatusCode(response.getStatus());
        command.setDurationMillis(durationMillis);
        command.setUserId(user == null ? null : user.userId());
        command.setAccountIdentifier(user == null ? null : user.userAccount());
        command.setIpAddress(NetUtils.getIpAddress(request));
        command.setThrowable(throwable);
        command.setEventTime(new Date());
        eventService.recordRequest(command);
    }
}
