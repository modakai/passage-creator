package com.sakura.passage_creator.notification.service;

import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.notification.model.entity.NotificationTarget;

import java.util.List;

/**
 * 通知目标服务。
 *
 * @author Sakura
 */
public interface NotificationTargetService extends IService<NotificationTarget> {

    /**
     * 替换通知目标。
     *
     * @param notificationId 通知 id
     * @param targetType 目标类型
     * @param roles 角色目标
     * @param userIds 用户目标
     */
    void replaceTargets(Long notificationId, String targetType, List<String> roles, List<Long> userIds);

    /**
     * 获取通知角色目标。
     *
     * @param notificationId 通知 id
     * @return 角色列表
     */
    List<String> listRoleTargets(Long notificationId);

    /**
     * 获取通知用户目标。
     *
     * @param notificationId 通知 id
     * @return 用户 id 列表
     */
    List<Long> listUserTargets(Long notificationId);
}
