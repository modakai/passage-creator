package com.sakura.passage_creator.notification.listener;

import com.sakura.passage_creator.notification.support.NotificationSendHelper;
import com.sakura.passage_creator.user.api.UserDisabledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听用户禁用事件，并把用户状态变化转换成站内通知。
 */
@Component
public class UserDisabledNotificationListener {

    /**
     * 通知发送辅助组件。
     */
    private final NotificationSendHelper notificationSendHelper;

    public UserDisabledNotificationListener(NotificationSendHelper notificationSendHelper) {
        this.notificationSendHelper = notificationSendHelper;
    }

    /**
     * 用户被禁用后发送禁用原因通知。
     *
     * @param event 用户禁用事件
     */
    @EventListener
    public void onUserDisabled(UserDisabledEvent event) {
        notificationSendHelper.sendUserDisabledNotification(event.userId(), event.disableReason());
    }
}
