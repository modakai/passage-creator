package com.sakura.passage_creator.notification.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.notification.model.entity.NotificationRead;
import com.sakura.passage_creator.notification.repository.NotificationReadMapper;
import com.sakura.passage_creator.notification.service.NotificationReadService;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.sakura.passage_creator.notification.model.entity.table.NotificationReadTableDef.NOTIFICATION_READ;

/**
 * 通知阅读状态服务实现。
 *
 * @author Sakura
 */
@Service
public class NotificationReadServiceImpl extends ServiceImpl<NotificationReadMapper, NotificationRead>
        implements NotificationReadService {

    @Override
    public void markRead(Long notificationId, String receiverType, Long userId) {
        NotificationRead read = getReadRecord(notificationId, receiverType, userId);
        if (read == null) {
            read = buildReadRecord(notificationId, receiverType, userId);
        }
        read.setReadTime(new Date());
        this.saveOrUpdate(read);
    }

    @Override
    public void markClosed(Long notificationId, String receiverType, Long userId) {
        NotificationRead read = getReadRecord(notificationId, receiverType, userId);
        if (read == null) {
            read = buildReadRecord(notificationId, receiverType, userId);
        }
        Date now = new Date();
        read.setCloseTime(now);
        if (read.getReadTime() == null) {
            read.setReadTime(now);
        }
        this.saveOrUpdate(read);
    }

    @Override
    public boolean isRead(Long notificationId, String receiverType, Long userId) {
        NotificationRead read = getReadRecord(notificationId, receiverType, userId);
        return read != null && read.getReadTime() != null;
    }

    @Override
    public boolean isClosed(Long notificationId, String receiverType, Long userId) {
        NotificationRead read = getReadRecord(notificationId, receiverType, userId);
        return read != null && read.getCloseTime() != null;
    }

    /**
     * 获取阅读记录。
     */
    private NotificationRead getReadRecord(Long notificationId, String receiverType, Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(NOTIFICATION_READ.NOTIFICATION_ID.eq(notificationId))
                .and(NOTIFICATION_READ.RECEIVER_TYPE.eq(receiverType))
                .and(NOTIFICATION_READ.USER_ID.eq(userId));
        return this.getOne(queryWrapper);
    }

    /**
     * 构造阅读记录。
     */
    private NotificationRead buildReadRecord(Long notificationId, String receiverType, Long userId) {
        NotificationRead read = new NotificationRead();
        read.setNotificationId(notificationId);
        read.setReceiverType(receiverType);
        read.setUserId(userId);
        return read;
    }
}
