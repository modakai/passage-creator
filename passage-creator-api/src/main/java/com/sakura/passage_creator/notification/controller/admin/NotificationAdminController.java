package com.sakura.passage_creator.notification.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.notification.model.dto.NotificationAddRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationAutoSendRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationQueryRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationUpdateRequest;
import com.sakura.passage_creator.notification.model.entity.Notification;
import com.sakura.passage_creator.notification.model.vo.NotificationVO;
import com.sakura.passage_creator.notification.service.NotificationService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端通知公告接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/notification")
@Validated
public class NotificationAdminController {

    @Resource
    private NotificationService notificationService;

    /**
     * 新增通知公告。
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addNotification(@Valid @RequestBody NotificationAddRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.addNotification(request, LoginUserContext.getLoginUser()));
    }

    /**
     * 更新草稿通知公告。
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateNotification(@Valid @RequestBody NotificationUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.updateNotification(request, LoginUserContext.getLoginUser()));
    }

    /**
     * 发布通知公告。
     */
    @PostMapping("/publish")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> publishNotification(@Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.publishNotification(request.getId(), LoginUserContext.getLoginUser()));
    }

    /**
     * 撤回通知公告。
     */
    @PostMapping("/revoke")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> revokeNotification(@Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.revokeNotification(request.getId(), LoginUserContext.getLoginUser()));
    }

    /**
     * 归档通知公告。
     */
    @PostMapping("/archive")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> archiveNotification(@Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.archiveNotification(request.getId(), LoginUserContext.getLoginUser()));
    }

    /**
     * 手动触发模板消息，用于系统事件接入前的验证和后台补发。
     */
    @PostMapping("/auto/send")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> sendByTemplate(@Valid @RequestBody NotificationAutoSendRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.sendByTemplate(request));
    }

    /**
     * 获取通知公告详情。
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<NotificationVO> getNotification(@RequestParam @Positive(message = "通知 id 必须大于 0") long id,
                                                        HttpServletRequest httpServletRequest) {
        Notification notification = notificationService.getById(id);
        ThrowUtils.throwIf(notification == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(notificationService.getNotificationVO(notification));
    }

    /**
     * 分页查询通知公告。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<NotificationVO>> listNotificationByPage(@Valid @RequestBody NotificationQueryRequest request,
            HttpServletRequest httpServletRequest) {
        long current = request.getPage();
        long pageSize = request.getPageSize();
        Page<Notification> page = notificationService.page(new Page<>(current, pageSize),
                notificationService.getQueryWrapper(request));
        List<NotificationVO> voList = notificationService.getNotificationVO(page.getRecords());
        Page<NotificationVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
