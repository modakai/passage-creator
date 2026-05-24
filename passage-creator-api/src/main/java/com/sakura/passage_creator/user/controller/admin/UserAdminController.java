package com.sakura.passage_creator.user.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.shared.enums.AuditOperationTypeEnum;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.annotation.AuditLogRecord;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import com.sakura.passage_creator.infrastructure.auth.PasswordHashService;
import com.sakura.passage_creator.user.api.UserDisabledEvent;
import com.sakura.passage_creator.user.model.dto.UserAddRequest;
import com.sakura.passage_creator.user.model.dto.UserQueryRequest;
import com.sakura.passage_creator.user.model.dto.UserUpdateRequest;
import com.sakura.passage_creator.user.model.entity.User;
import com.sakura.passage_creator.user.service.UserService;
import io.github.linpeilie.Converter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台用户管理接口。
 *
 * 作者：Sakura
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserAdminController {

    @Resource
    private UserService userService;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private PasswordHashService passwordHashService;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    @Resource
    private Converter converter;

    /**
     * 创建用户。
     *
     * @param userAddRequest 创建请求
     * @param request HTTP 请求
     * @return 新用户 id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "创建用户", module = "用户管理", operationType = AuditOperationTypeEnum.CREATE)
    public BaseResponse<Long> addUser(@Valid @RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        User user = converter.convert(userAddRequest, User.class);

        // 后台创建用户必须由管理员指定初始密码，不再落入固定默认密码。
        user.setUserPassword(passwordHashService.hash(userAddRequest.getUserPassword()));

        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户。
     *
     * @param deleteRequest 删除请求
     * @param request HTTP 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "删除用户", module = "用户管理", operationType = AuditOperationTypeEnum.DELETE)
    public BaseResponse<Boolean> deleteUser(@Valid @RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        boolean result = userService.removeUser(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户。
     *
     * @param userUpdateRequest 更新请求
     * @param request HTTP 请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "更新用户", module = "用户管理", operationType = AuditOperationTypeEnum.UPDATE)
    public BaseResponse<Boolean> updateUser(@Valid @RequestBody UserUpdateRequest userUpdateRequest,
            HttpServletRequest request) {
        boolean result = userService.updateUser(userUpdateRequest);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        sendDisabledNotificationIfNeeded(userUpdateRequest);
        return ResultUtils.success(true);
    }

    /**
     * 用户被禁用时按模板发送系统消息。
     *
     * @param request 用户更新请求
     */
    private void sendDisabledNotificationIfNeeded(UserUpdateRequest request) {
        if (!UserConstant.STATUS_DISABLED.equals(request.getStatus())) {
            return;
        }
        applicationEventPublisher.publishEvent(new UserDisabledEvent(request.getId(), request.getDisableReason()));
    }

    /**
     * 根据 id 获取用户。
     *
     * @param id 用户 id
     * @param request HTTP 请求
     * @return 用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestParam @Positive(message = "用户 id 必须大于 0") long id,
            HttpServletRequest request) {
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 分页查询用户。
     *
     * @param userQueryRequest 查询请求
     * @param request HTTP 请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@Valid @RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        long current = userQueryRequest.getPage();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 管理员重置用户密码。
     *
     * @param deleteRequest 重置请求，仅使用用户 id
     * @param request HTTP 请求
     * @return 是否重置成功
     */
    @PostMapping("/reset/password")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "重置用户密码", module = "用户管理", operationType = AuditOperationTypeEnum.UPDATE)
    public BaseResponse<String> resetUserPassword(@Valid @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        String temporaryPassword = userService.resetPassword(deleteRequest.getId());
        return ResultUtils.success(temporaryPassword);
    }
}
