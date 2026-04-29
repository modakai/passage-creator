package com.sakura.passage_creator.notification.support;

import com.sakura.passage_creator.notification.enums.NotificationReceiverTypeEnum;
import com.sakura.passage_creator.notification.model.dto.NotificationAutoSendRequest;
import com.sakura.passage_creator.notification.service.NotificationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知发送封装，负责把业务事件转换成模板通知请求。
 *
 * @author Sakura
 */
@Component
public class NotificationSendHelper {

    /**
     * 用户禁用事件编码。
     */
    private static final String USER_DISABLED_EVENT = "user_disabled";

    /**
     * 禁用原因模板变量名。
     */
    private static final String REASON_VARIABLE = "reason";

    /**
     * 禁用原因兜底文案。
     */
    private static final String DEFAULT_DISABLE_REASON = "未填写原因";

    /**
     * 通知服务。
     */
    private final NotificationService notificationService;

    public NotificationSendHelper(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 发送用户禁用通知。
     *
     * @param userId 被禁用用户 id
     * @param disableReason 禁用原因
     */
    public void sendUserDisabledNotification(Long userId, String disableReason) {
        NotificationAutoSendRequest request = new NotificationAutoSendRequest();
        request.setEventType(USER_DISABLED_EVENT);
        request.setReceiverType(NotificationReceiverTypeEnum.APP.getValue());
        request.setTargetUserId(userId);
        request.setVariables(Map.of(REASON_VARIABLE, resolveDisableReason(disableReason)));
        notificationService.sendByTemplate(request);
    }

    /**
     * 获取禁用原因，没有填写时使用固定兜底文案。
     *
     * @param disableReason 禁用原因
     * @return 可用于模板渲染的禁用原因
     */
    private String resolveDisableReason(String disableReason) {
        return StringUtils.isBlank(disableReason) ? DEFAULT_DISABLE_REASON : disableReason;
    }
}
