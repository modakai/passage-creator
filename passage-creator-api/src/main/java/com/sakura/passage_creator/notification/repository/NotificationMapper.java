package com.sakura.passage_creator.notification.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.notification.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知公告 Mapper。
 *
 * @author Sakura
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
