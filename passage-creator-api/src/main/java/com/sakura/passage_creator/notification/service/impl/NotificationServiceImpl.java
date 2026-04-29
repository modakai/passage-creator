package com.sakura.passage_creator.notification.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.notification.enums.NotificationReceiverTypeEnum;
import com.sakura.passage_creator.notification.enums.NotificationStatusEnum;
import com.sakura.passage_creator.notification.enums.NotificationTargetTypeEnum;
import com.sakura.passage_creator.notification.enums.NotificationTypeEnum;
import com.sakura.passage_creator.notification.model.dto.NotificationAddRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationAutoSendRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationQueryRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTargetContext;
import com.sakura.passage_creator.notification.model.dto.NotificationUpdateRequest;
import com.sakura.passage_creator.notification.model.entity.Notification;
import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import com.sakura.passage_creator.notification.model.vo.NotificationVO;
import com.sakura.passage_creator.notification.repository.NotificationMapper;
import com.sakura.passage_creator.notification.service.NotificationReadService;
import com.sakura.passage_creator.notification.service.NotificationService;
import com.sakura.passage_creator.notification.service.NotificationTargetService;
import com.sakura.passage_creator.notification.service.NotificationTemplateService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.notification.model.entity.table.NotificationTableDef.NOTIFICATION;

/**
 * 通知公告服务实现。
 *
 * @author Sakura
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Resource
    private NotificationTargetService notificationTargetService;

    @Resource
    private NotificationReadService notificationReadService;

    @Resource
    private NotificationTemplateService notificationTemplateService;

    @Resource
    private NotificationTemplateRenderer templateRenderer;

    @Resource
    private NotificationDomainServiceImpl domainService;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    @Resource
    private Converter converter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addNotification(NotificationAddRequest request, LoginUserInfo operator) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Notification notification = converter.convert(request, Notification.class);
        notification.setStatus(NotificationStatusEnum.DRAFT.getValue());
        if (operator != null) {
            notification.setCreateUserId(operator.userId());
            notification.setUpdateUserId(operator.userId());
        }
        fillDefaultDisplay(notification);
        validNotification(notification);
        boolean result = this.save(notification);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        notificationTargetService.replaceTargets(notification.getId(), notification.getTargetType(),
                request.getTargetRoles(), request.getTargetUserIds());
        return notification.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateNotification(NotificationUpdateRequest request, LoginUserInfo operator) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Notification oldNotification = this.getById(request.getId());
        ThrowUtils.throwIf(oldNotification == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!NotificationStatusEnum.DRAFT.getValue().equals(oldNotification.getStatus()),
                ErrorCode.OPERATION_ERROR, "notification.only_draft_can_update");
        Notification notification = converter.convert(request, Notification.class);
        if (operator != null) {
            notification.setUpdateUserId(operator.userId());
        }
        fillDefaultDisplay(notification);
        validNotification(notification);
        boolean result = this.updateById(notification);
        notificationTargetService.replaceTargets(notification.getId(), notification.getTargetType(),
                request.getTargetRoles(), request.getTargetUserIds());
        return result;
    }

    @Override
    public boolean publishNotification(Long id, LoginUserInfo operator) {
        Notification notification = assertNotificationExists(id);
        domainService.assertCanPublish(notification);
        Notification update = new Notification();
        update.setId(id);
        update.setStatus(NotificationStatusEnum.PUBLISHED.getValue());
        update.setPublishTime(new Date());
        update.setPublisherId(operator == null ? null : operator.userId());
        update.setUpdateUserId(operator == null ? null : operator.userId());
        return this.updateById(update);
    }

    @Override
    public boolean revokeNotification(Long id, LoginUserInfo operator) {
        Notification notification = assertNotificationExists(id);
        ThrowUtils.throwIf(!NotificationStatusEnum.PUBLISHED.getValue().equals(notification.getStatus()),
                ErrorCode.OPERATION_ERROR, "notification.only_published_can_revoke");
        Notification update = new Notification();
        update.setId(id);
        update.setStatus(NotificationStatusEnum.REVOKED.getValue());
        update.setUpdateUserId(operator == null ? null : operator.userId());
        return this.updateById(update);
    }

    @Override
    public boolean archiveNotification(Long id, LoginUserInfo operator) {
        assertNotificationExists(id);
        Notification update = new Notification();
        update.setId(id);
        update.setStatus(NotificationStatusEnum.ARCHIVED.getValue());
        update.setUpdateUserId(operator == null ? null : operator.userId());
        return this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendByTemplate(NotificationAutoSendRequest request) {
        ThrowUtils.throwIf(request == null || request.getTargetUserId() == null, ErrorCode.PARAMS_ERROR);
        NotificationTemplate template = notificationTemplateService.getEnabledByEventType(request.getEventType());
        if (template == null) {
            return null;
        }
        String title = templateRenderer.render(template.getTitleTemplate(), template.getVariableSchema(), request.getVariables());
        String content = templateRenderer.render(template.getContentTemplate(), template.getVariableSchema(), request.getVariables());
        NotificationAddRequest addRequest = new NotificationAddRequest();
        addRequest.setType(NotificationTypeEnum.MESSAGE.getValue());
        addRequest.setTitle(title);
        addRequest.setContent(content);
        addRequest.setLevel("info");
        addRequest.setReceiverType(StringUtils.defaultIfBlank(request.getReceiverType(), template.getReceiverType()));
        addRequest.setTargetType(NotificationTargetTypeEnum.USER.getValue());
        addRequest.setTargetUserIds(List.of(request.getTargetUserId()));
        Long notificationId = addNotification(addRequest, null);
        publishNotification(notificationId, null);
        return notificationId;
    }

    @Override
    public QueryWrapper getQueryWrapper(NotificationQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(NOTIFICATION.ID.eq(request.getId(), request.getId() != null));
        queryWrapper.and(NOTIFICATION.TYPE.eq(request.getType(), StringUtils.isNotBlank(request.getType())));
        queryWrapper.and(NOTIFICATION.TITLE.like(request.getTitle(), StringUtils.isNotBlank(request.getTitle())));
        queryWrapper.and(NOTIFICATION.STATUS.eq(request.getStatus(), StringUtils.isNotBlank(request.getStatus())));
        queryWrapper.and(NOTIFICATION.RECEIVER_TYPE.eq(request.getReceiverType(),
                StringUtils.isNotBlank(request.getReceiverType())));
        queryWrapper.and(NOTIFICATION.TARGET_TYPE.eq(request.getTargetType(), StringUtils.isNotBlank(request.getTargetType())));
        queryWrapper.and(NOTIFICATION.PUBLISH_TIME.ge(request.getPublishStartTime(), request.getPublishStartTime() != null));
        queryWrapper.and(NOTIFICATION.PUBLISH_TIME.le(request.getPublishEndTime(), request.getPublishEndTime() != null));
        QueryColumn sortColumn = resolveSortColumn(request.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(request.getSortOrder()));
        } else {
            queryWrapper.orderBy(NOTIFICATION.ID, false);
        }
        return queryWrapper;
    }

    /**
     * 将客户端排序字段转换为通知表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> NOTIFICATION.ID;
            case "type" -> NOTIFICATION.TYPE;
            case "title" -> NOTIFICATION.TITLE;
            case "status" -> NOTIFICATION.STATUS;
            case "receiver_type" -> NOTIFICATION.RECEIVER_TYPE;
            case "target_type" -> NOTIFICATION.TARGET_TYPE;
            case "pinned" -> NOTIFICATION.PINNED;
            case "publish_time" -> NOTIFICATION.PUBLISH_TIME;
            case "effective_time" -> NOTIFICATION.EFFECTIVE_TIME;
            case "expire_time" -> NOTIFICATION.EXPIRE_TIME;
            case "create_time" -> NOTIFICATION.CREATE_TIME;
            case "update_time" -> NOTIFICATION.UPDATE_TIME;
            default -> null;
        };
    }

    @Override
    public List<NotificationVO> listVisibleNotifications(String receiverType, LoginUserInfo user, String type) {
        ThrowUtils.throwIf(user == null || user.userId() == null, ErrorCode.NOT_LOGIN_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(NOTIFICATION.STATUS.eq(NotificationStatusEnum.PUBLISHED.getValue()))
                .and(NOTIFICATION.TYPE.eq(type, StringUtils.isNotBlank(type)))
                .orderBy(NOTIFICATION.PINNED, false)
                .orderBy(NOTIFICATION.PUBLISH_TIME, false)
                .orderBy(NOTIFICATION.ID, false);
        Date now = new Date();
        return this.list(queryWrapper).stream()
                .filter(notification -> NotificationReceiverTypeEnum.matches(notification.getReceiverType(), receiverType))
                .filter(notification -> notification.getEffectiveTime() == null || !notification.getEffectiveTime().after(now))
                .filter(notification -> notification.getExpireTime() == null || !notification.getExpireTime().before(now))
                .filter(notification -> isVisible(notification, receiverType, user))
                .filter(notification -> !NotificationTypeEnum.ANNOUNCEMENT.getValue().equals(notification.getType())
                        || !notificationReadService.isClosed(notification.getId(), receiverType, user.userId()))
                .map(notification -> {
                    NotificationVO vo = getNotificationVO(notification);
                    vo.setRead(notificationReadService.isRead(notification.getId(), receiverType, user.userId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public long countUnreadMessages(String receiverType, LoginUserInfo user) {
        return listVisibleNotifications(receiverType, user, NotificationTypeEnum.MESSAGE.getValue()).stream()
                .filter(vo -> !Boolean.TRUE.equals(vo.getRead()))
                .count();
    }

    @Override
    public boolean markRead(Long notificationId, String receiverType, LoginUserInfo user) {
        Notification notification = assertNotificationExists(notificationId);
        ThrowUtils.throwIf(!isVisible(notification, receiverType, user), ErrorCode.NO_AUTH_ERROR);
        notificationReadService.markRead(notificationId, receiverType, user.userId());
        return true;
    }

    @Override
    public boolean markAllRead(String receiverType, LoginUserInfo user) {
        for (NotificationVO vo : listVisibleNotifications(receiverType, user, NotificationTypeEnum.MESSAGE.getValue())) {
            notificationReadService.markRead(vo.getId(), receiverType, user.userId());
        }
        return true;
    }

    @Override
    public boolean closeAnnouncement(Long notificationId, String receiverType, LoginUserInfo user) {
        Notification notification = assertNotificationExists(notificationId);
        ThrowUtils.throwIf(!NotificationTypeEnum.ANNOUNCEMENT.getValue().equals(notification.getType()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!isVisible(notification, receiverType, user), ErrorCode.NO_AUTH_ERROR);
        notificationReadService.markClosed(notificationId, receiverType, user.userId());
        return true;
    }

    @Override
    public NotificationVO getNotificationVO(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationVO vo = converter.convert(notification, NotificationVO.class);
        vo.setTargetRoles(notificationTargetService.listRoleTargets(notification.getId()));
        vo.setTargetUserIds(notificationTargetService.listUserTargets(notification.getId()));
        return vo;
    }

    @Override
    public List<NotificationVO> getNotificationVO(List<Notification> notificationList) {
        if (CollUtil.isEmpty(notificationList)) {
            return new ArrayList<>();
        }
        return notificationList.stream().map(this::getNotificationVO).collect(Collectors.toList());
    }

    /**
     * 判断通知是否对用户可见。
     */
    private boolean isVisible(Notification notification, String receiverType, LoginUserInfo user) {
        return domainService.isVisibleTo(notification, new NotificationTargetContext(receiverType, user),
                notificationTargetService.listRoleTargets(notification.getId()),
                notificationTargetService.listUserTargets(notification.getId()));
    }

    /**
     * 断言通知存在。
     */
    private Notification assertNotificationExists(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Notification notification = this.getById(id);
        ThrowUtils.throwIf(notification == null, ErrorCode.NOT_FOUND_ERROR);
        return notification;
    }

    /**
     * 填充展示配置默认值。
     */
    private void fillDefaultDisplay(Notification notification) {
        if (notification.getPinned() == null) {
            notification.setPinned(0);
        }
        if (notification.getPopup() == null) {
            notification.setPopup(0);
        }
        if (StringUtils.isBlank(notification.getLevel())) {
            notification.setLevel("info");
        }
    }

    /**
     * 校验通知公告参数。
     */
    private void validNotification(Notification notification) {
        if (StringUtils.isAnyBlank(notification.getType(), notification.getTitle(), notification.getContent(),
                notification.getReceiverType(), notification.getTargetType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }
}
