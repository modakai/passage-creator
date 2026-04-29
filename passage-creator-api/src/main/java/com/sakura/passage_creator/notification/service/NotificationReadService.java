package com.sakura.passage_creator.notification.service;

import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.notification.model.entity.NotificationRead;

/**
 * 通知阅读状态服务。
 *
 * @author Sakura
 */
public interface NotificationReadService extends IService<NotificationRead> {

    /**
     * 标记已读。
     *
     * @param notificationId 通知 id
     * @param receiverType 接收端
     * @param userId 用户 id
     */
    void markRead(Long notificationId, String receiverType, Long userId);

    /**
     * 标记关闭。
     *
     * @param notificationId 通知 id
     * @param receiverType 接收端
     * @param userId 用户 id
     */
    void markClosed(Long notificationId, String receiverType, Long userId);

    /**
     * 判断是否已读。
     *
     * @param notificationId 通知 id
     * @param receiverType 接收端
     * @param userId 用户 id
     * @return 是否已读
     */
    boolean isRead(Long notificationId, String receiverType, Long userId);

    /**
     * 判断是否已关闭。
     *
     * @param notificationId 通知 id
     * @param receiverType 接收端
     * @param userId 用户 id
     * @return 是否已关闭
     */
    boolean isClosed(Long notificationId, String receiverType, Long userId);
}
