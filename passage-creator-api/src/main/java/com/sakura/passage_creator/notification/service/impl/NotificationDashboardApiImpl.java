package com.sakura.passage_creator.notification.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.notification.api.NotificationDashboardApi;
import com.sakura.passage_creator.notification.enums.NotificationReceiverTypeEnum;
import com.sakura.passage_creator.notification.enums.NotificationStatusEnum;
import com.sakura.passage_creator.notification.service.NotificationService;
import org.springframework.stereotype.Component;

import static com.sakura.passage_creator.notification.model.entity.table.NotificationTableDef.NOTIFICATION;

/**
 * Dashboard 通知统计 API 实现。
 */
@Component
public class NotificationDashboardApiImpl implements NotificationDashboardApi {

    private final NotificationService notificationService;

    public NotificationDashboardApiImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public long countAdminVisibleNotifications() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                // Dashboard 通知数量只统计管理员端可见且已发布的通知，避免草稿污染首页指标。
                .where(NOTIFICATION.STATUS.eq(NotificationStatusEnum.PUBLISHED.getValue()))
                .and(NOTIFICATION.RECEIVER_TYPE.in(NotificationReceiverTypeEnum.ADMIN.getValue(),
                        NotificationReceiverTypeEnum.ALL.getValue()));
        return notificationService.count(queryWrapper);
    }
}
