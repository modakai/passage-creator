package com.sakura.passage_creator.notification.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.notification.enums.NotificationTargetTypeEnum;
import com.sakura.passage_creator.notification.model.entity.NotificationTarget;
import com.sakura.passage_creator.notification.repository.NotificationTargetMapper;
import com.sakura.passage_creator.notification.service.NotificationTargetService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.notification.model.entity.table.NotificationTargetTableDef.NOTIFICATION_TARGET;

/**
 * 通知目标服务实现。
 *
 * @author Sakura
 */
@Service
public class NotificationTargetServiceImpl extends ServiceImpl<NotificationTargetMapper, NotificationTarget>
        implements NotificationTargetService {

    @Override
    public void replaceTargets(Long notificationId, String targetType, List<String> roles, List<Long> userIds) {
        QueryWrapper deleteWrapper = QueryWrapper.create().where(NOTIFICATION_TARGET.NOTIFICATION_ID.eq(notificationId));
        this.remove(deleteWrapper);
        if (NotificationTargetTypeEnum.ROLE.getValue().equals(targetType) && CollUtil.isNotEmpty(roles)) {
            this.saveBatch(roles.stream().map(role -> buildTarget(notificationId, targetType, role)).collect(Collectors.toList()));
        }
        if (NotificationTargetTypeEnum.USER.getValue().equals(targetType) && CollUtil.isNotEmpty(userIds)) {
            this.saveBatch(userIds.stream().map(userId -> buildTarget(notificationId, targetType, String.valueOf(userId)))
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public List<String> listRoleTargets(Long notificationId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(NOTIFICATION_TARGET.NOTIFICATION_ID.eq(notificationId))
                .and(NOTIFICATION_TARGET.TARGET_TYPE.eq(NotificationTargetTypeEnum.ROLE.getValue()));
        return this.list(queryWrapper).stream().map(NotificationTarget::getTargetValue).collect(Collectors.toList());
    }

    @Override
    public List<Long> listUserTargets(Long notificationId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(NOTIFICATION_TARGET.NOTIFICATION_ID.eq(notificationId))
                .and(NOTIFICATION_TARGET.TARGET_TYPE.eq(NotificationTargetTypeEnum.USER.getValue()));
        List<Long> result = new ArrayList<>();
        for (NotificationTarget target : this.list(queryWrapper)) {
            result.add(Long.valueOf(target.getTargetValue()));
        }
        return result;
    }

    /**
     * 构造目标记录。
     *
     * @param notificationId 通知 id
     * @param targetType 目标类型
     * @param targetValue 目标值
     * @return 目标实体
     */
    private NotificationTarget buildTarget(Long notificationId, String targetType, String targetValue) {
        NotificationTarget target = new NotificationTarget();
        target.setNotificationId(notificationId);
        target.setTargetType(targetType);
        target.setTargetValue(targetValue);
        return target;
    }
}
