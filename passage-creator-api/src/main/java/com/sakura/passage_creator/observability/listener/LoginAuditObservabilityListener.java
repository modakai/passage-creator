package com.sakura.passage_creator.observability.listener;

import com.sakura.passage_creator.audit.api.LoginAuditCommand;
import com.sakura.passage_creator.audit.api.LoginAuditSubmittedEvent;
import com.sakura.passage_creator.observability.api.ObservabilityAlertRaisedEvent;
import com.sakura.passage_creator.observability.config.ObservabilityProperties;
import com.sakura.passage_creator.observability.enums.ObservabilityEventLevelEnum;
import com.sakura.passage_creator.observability.enums.ObservabilityEventTypeEnum;
import com.sakura.passage_creator.observability.model.entity.ObservabilityEvent;
import com.sakura.passage_creator.observability.service.ObservabilityEventService;
import com.sakura.passage_creator.observability.service.impl.LoginFailureWindowService;
import com.sakura.passage_creator.shared.util.RedisUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录审计观测监听器，负责登录失败统计和异常 IP 告警。
 *
 * @author Sakura
 */
@Component
public class LoginAuditObservabilityListener {

    /**
     * 告警冷却 Redis key 前缀。
     */
    private static final String ALERT_COOLDOWN_PREFIX = "observability:alert-cooldown:";

    /**
     * 登录失败统计服务。
     */
    private final LoginFailureWindowService loginFailureWindowService;

    /**
     * 运维事件服务。
     */
    private final ObservabilityEventService eventService;

    /**
     * 可观测性配置。
     */
    private final ObservabilityProperties properties;

    /**
     * Spring 事件发布器。
     */
    private final ApplicationEventPublisher eventPublisher;

    public LoginAuditObservabilityListener(LoginFailureWindowService loginFailureWindowService,
            ObservabilityEventService eventService, ObservabilityProperties properties,
            ApplicationEventPublisher eventPublisher) {
        this.loginFailureWindowService = loginFailureWindowService;
        this.eventService = eventService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 监听登录审计事实，只处理失败登录。
     */
    @EventListener
    public void onLoginAuditSubmitted(LoginAuditSubmittedEvent event) {
        LoginAuditCommand command = event.command();
        if (command == null || command.success()) {
            return;
        }
        long ipCount = loginFailureWindowService.incrementIpFailure(command.ipAddress());
        long accountCount = loginFailureWindowService.incrementAccountFailure(command.accountIdentifier());
        saveLoginFailureEvent(command, ipCount, accountCount);
        if (loginFailureWindowService.reachesIpThreshold(ipCount)) {
            raiseSecurityAlert(command, "login_failure_ip", command.ipAddress(), ipCount);
        }
        if (loginFailureWindowService.reachesAccountThreshold(accountCount)) {
            raiseSecurityAlert(command, "login_failure_account", command.accountIdentifier(), accountCount);
        }
    }

    /**
     * 保存单次登录失败事件。
     */
    private void saveLoginFailureEvent(LoginAuditCommand command, long ipCount, long accountCount) {
        ObservabilityEvent event = new ObservabilityEvent();
        event.setEventType(ObservabilityEventTypeEnum.LOGIN_FAILURE.getValue());
        event.setEventLevel(ObservabilityEventLevelEnum.WARNING.getValue());
        event.setTitle("登录失败");
        event.setSubject(command.ipAddress());
        event.setUserId(command.userId());
        event.setAccountIdentifier(command.accountIdentifier());
        event.setIpAddress(command.ipAddress());
        event.setDurationMillis(command.costMillis());
        event.setDetail("IP 失败次数：" + ipCount + "，账号失败次数：" + accountCount + "，原因：" + command.failureReason());
        event.setEventTime(new Date());
        eventService.saveEventAsync(event);
    }

    /**
     * 触发安全告警并做冷却去重。
     */
    private void raiseSecurityAlert(LoginAuditCommand command, String ruleCode, String subject, long count) {
        String cooldownKey = ALERT_COOLDOWN_PREFIX + ruleCode + ":" + subject;
        if (Boolean.TRUE.equals(RedisUtil.hasKey(cooldownKey))) {
            return;
        }
        RedisUtil.setCacheObject(cooldownKey, "1", properties.getAlertCooldownSeconds(), TimeUnit.SECONDS);
        ObservabilityEvent event = new ObservabilityEvent();
        event.setEventType(ObservabilityEventTypeEnum.ABNORMAL_IP.getValue());
        event.setEventLevel(ObservabilityEventLevelEnum.ERROR.getValue());
        event.setTitle("高频登录失败");
        event.setSubject(subject);
        event.setAccountIdentifier(command.accountIdentifier());
        event.setIpAddress(command.ipAddress());
        event.setDetail("规则：" + ruleCode + "，主体：" + subject + "，窗口内失败次数：" + count);
        event.setEventTime(new Date());
        eventService.saveEventAsync(event);
        eventPublisher.publishEvent(new ObservabilityAlertRaisedEvent(
                ruleCode,
                subject,
                "高频登录失败",
                "检测到 " + subject + " 在短时间内登录失败 " + count + " 次，请及时检查。",
                ObservabilityEventLevelEnum.ERROR.getValue(),
                Map.of("subject", subject, "count", count, "ipAddress", command.ipAddress()),
                new Date()
        ));
    }
}
