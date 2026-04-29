package com.sakura.passage_creator.notification.api;

/**
 * 通知模块供 Dashboard 使用的窄口径查询 API。
 */
public interface NotificationDashboardApi {

    /**
     * 统计后台管理员当前可见的已发布通知数量。
     *
     * @return 通知数量
     */
    long countAdminVisibleNotifications();
}
