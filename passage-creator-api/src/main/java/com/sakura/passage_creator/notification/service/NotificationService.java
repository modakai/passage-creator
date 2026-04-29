package com.sakura.passage_creator.notification.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.notification.model.dto.NotificationAddRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationAutoSendRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationQueryRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationUpdateRequest;
import com.sakura.passage_creator.notification.model.entity.Notification;
import com.sakura.passage_creator.notification.model.vo.NotificationVO;
import com.sakura.passage_creator.shared.context.LoginUserInfo;

import java.util.List;

/**
 * 通知公告服务。
 *
 * @author Sakura
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 新增通知。
     */
    Long addNotification(NotificationAddRequest request, LoginUserInfo operator);

    /**
     * 更新通知。
     */
    boolean updateNotification(NotificationUpdateRequest request, LoginUserInfo operator);

    /**
     * 发布通知。
     */
    boolean publishNotification(Long id, LoginUserInfo operator);

    /**
     * 撤回通知。
     */
    boolean revokeNotification(Long id, LoginUserInfo operator);

    /**
     * 归档通知。
     */
    boolean archiveNotification(Long id, LoginUserInfo operator);

    /**
     * 自动模板发送。
     */
    Long sendByTemplate(NotificationAutoSendRequest request);

    /**
     * 构造管理端查询条件。
     */
    QueryWrapper getQueryWrapper(NotificationQueryRequest request);

    /**
     * 获取可见通知列表。
     */
    List<NotificationVO> listVisibleNotifications(String receiverType, LoginUserInfo user, String type);

    /**
     * 获取未读消息数量。
     */
    long countUnreadMessages(String receiverType, LoginUserInfo user);

    /**
     * 标记单条已读。
     */
    boolean markRead(Long notificationId, String receiverType, LoginUserInfo user);

    /**
     * 标记全部已读。
     */
    boolean markAllRead(String receiverType, LoginUserInfo user);

    /**
     * 关闭公告弹窗。
     */
    boolean closeAnnouncement(Long notificationId, String receiverType, LoginUserInfo user);

    /**
     * 转换返回对象。
     */
    NotificationVO getNotificationVO(Notification notification);

    /**
     * 转换返回对象列表。
     */
    List<NotificationVO> getNotificationVO(List<Notification> notificationList);
}
