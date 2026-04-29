package com.sakura.passage_creator.notification.repository;

import com.mybatisflex.core.BaseMapper;
import com.sakura.passage_creator.notification.model.entity.NotificationTarget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知目标 Mapper。
 *
 * @author Sakura
 */
@Mapper
public interface NotificationTargetMapper extends BaseMapper<NotificationTarget> {
}
