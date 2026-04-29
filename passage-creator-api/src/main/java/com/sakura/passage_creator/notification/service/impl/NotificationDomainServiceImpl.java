package com.sakura.passage_creator.notification.service.impl;

import com.sakura.passage_creator.notification.enums.NotificationReceiverTypeEnum;
import com.sakura.passage_creator.notification.enums.NotificationStatusEnum;
import com.sakura.passage_creator.notification.enums.NotificationTargetTypeEnum;
import com.sakura.passage_creator.notification.model.dto.NotificationTargetContext;
import com.sakura.passage_creator.notification.model.entity.Notification;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 通知领域规则服务。
 *
 * @author Sakura
 */
@Component
public class NotificationDomainServiceImpl {

    /**
     * 判断通知是否对当前上下文可见。
     *
     * @param notification 通知实体
     * @param context 当前用户上下文
     * @return 是否可见
     */
    public boolean isVisibleTo(Notification notification, NotificationTargetContext context) {
        return isVisibleTo(notification, context, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * 判断通知是否对当前上下文可见。
     *
     * @param notification 通知实体
     * @param context 当前用户上下文
     * @param roleTargets 角色目标列表
     * @param userTargets 用户目标列表
     * @return 是否可见
     */
    public boolean isVisibleTo(Notification notification, NotificationTargetContext context,
            List<String> roleTargets, List<Long> userTargets) {
        if (notification == null || context == null || context.getUser() == null) {
            return false;
        }
        if (!NotificationStatusEnum.PUBLISHED.getValue().equals(notification.getStatus())) {
            return false;
        }
        if (!NotificationReceiverTypeEnum.matches(notification.getReceiverType(), context.getReceiverType())) {
            return false;
        }
        return matchTarget(notification.getTargetType(), context.getUser(), roleTargets, userTargets);
    }

    /**
     * 发布前校验通知状态。
     *
     * @param notification 通知实体
     */
    public void assertCanPublish(Notification notification) {
        if (notification == null || notification.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "notification.not_found");
        }
        String status = notification.getStatus();
        if (NotificationStatusEnum.REVOKED.getValue().equals(status)
                || NotificationStatusEnum.ARCHIVED.getValue().equals(status)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "notification.status.cannot_publish");
        }
    }

    /**
     * 判断目标范围是否命中当前用户。
     *
     * @param targetType 目标范围类型
     * @param user 当前用户快照
     * @param roleTargets 角色目标列表
     * @param userTargets 用户目标列表
     * @return 是否命中
     */
    private boolean matchTarget(String targetType, LoginUserInfo user, List<String> roleTargets, List<Long> userTargets) {
        if (NotificationTargetTypeEnum.ALL.getValue().equals(targetType)) {
            return true;
        }
        if (NotificationTargetTypeEnum.ROLE.getValue().equals(targetType)) {
            return roleTargets != null && roleTargets.stream().anyMatch(role -> StringUtils.equals(role, user.userRole()));
        }
        if (NotificationTargetTypeEnum.USER.getValue().equals(targetType)) {
            return userTargets != null && userTargets.contains(user.userId());
        }
        return false;
    }
}
