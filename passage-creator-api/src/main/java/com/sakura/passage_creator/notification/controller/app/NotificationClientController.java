package com.sakura.passage_creator.notification.controller.app;

import com.sakura.passage_creator.notification.model.vo.NotificationVO;
import com.sakura.passage_creator.notification.service.NotificationService;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台端和用户端通知查询接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/notification/client")
@Validated
public class NotificationClientController {

    @Resource
    private NotificationService notificationService;

    /**
     * 查询当前端通知消息。
     */
    @GetMapping("/messages")
    public BaseResponse<List<NotificationVO>> listMessages(@RequestParam String receiverType,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.listVisibleNotifications(receiverType,
                LoginUserContext.getLoginUser(), "message"));
    }

    /**
     * 查询当前端公告。
     */
    @GetMapping("/announcements")
    public BaseResponse<List<NotificationVO>> listAnnouncements(@RequestParam String receiverType,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.listVisibleNotifications(receiverType,
                LoginUserContext.getLoginUser(), "announcement"));
    }

    /**
     * 查询当前端未读消息数。
     */
    @GetMapping("/unread/count")
    public BaseResponse<Long> countUnread(@RequestParam String receiverType, HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.countUnreadMessages(receiverType, LoginUserContext.getLoginUser()));
    }

    /**
     * 标记单条通知已读。
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> markRead(@RequestParam String receiverType,
            @Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.markRead(request.getId(), receiverType, LoginUserContext.getLoginUser()));
    }

    /**
     * 标记全部消息已读。
     */
    @PostMapping("/read/all")
    public BaseResponse<Boolean> markAllRead(@RequestParam String receiverType, HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.markAllRead(receiverType, LoginUserContext.getLoginUser()));
    }

    /**
     * 关闭公告弹窗。
     */
    @PostMapping("/announcement/close")
    public BaseResponse<Boolean> closeAnnouncement(@RequestParam String receiverType,
            @Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(notificationService.closeAnnouncement(request.getId(), receiverType,
                LoginUserContext.getLoginUser()));
    }
}
