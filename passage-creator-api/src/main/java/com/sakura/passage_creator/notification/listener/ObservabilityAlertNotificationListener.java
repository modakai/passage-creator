package com.sakura.passage_creator.notification.listener;

import com.sakura.passage_creator.notification.enums.NotificationReceiverTypeEnum;
import com.sakura.passage_creator.notification.enums.NotificationTargetTypeEnum;
import com.sakura.passage_creator.notification.enums.NotificationTypeEnum;
import com.sakura.passage_creator.notification.model.dto.NotificationAddRequest;
import com.sakura.passage_creator.notification.service.NotificationService;
import com.sakura.passage_creator.observability.api.ObservabilityAlertRaisedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 可观测性告警通知监听器，把高风险运维事件转换成管理员站内通知。
 *
 * @author Sakura
 */
@Component
@Slf4j
public class ObservabilityAlertNotificationListener {

    /**
     * 通知服务。
     */
    private final NotificationService notificationService;

    public ObservabilityAlertNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 收到可观测性告警后生成管理员通知。
     */
    @EventListener
    public void onObservabilityAlertRaised(ObservabilityAlertRaisedEvent event) {
        try {
            NotificationAddRequest request = new NotificationAddRequest();
            request.setType(NotificationTypeEnum.MESSAGE.getValue());
            request.setTitle(event.title());
            request.setSummary(event.subject());
            request.setContent(event.content());
            request.setLevel(event.level());
            request.setReceiverType(NotificationReceiverTypeEnum.ADMIN.getValue());
            request.setTargetType(NotificationTargetTypeEnum.ALL.getValue());
            Long notificationId = notificationService.addNotification(request, null);
            notificationService.publishNotification(notificationId, null);
        } catch (Exception e) {
            log.error("send observability alert notification failed", e);
        }
    }
}
