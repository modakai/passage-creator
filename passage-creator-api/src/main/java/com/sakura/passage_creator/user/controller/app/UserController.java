package com.sakura.passage_creator.user.controller.app;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import com.sakura.passage_creator.user.model.dto.UserQueryRequest;
import com.sakura.passage_creator.user.model.dto.UserUpdateMyRequest;
import com.sakura.passage_creator.user.model.dto.UserUpdatePasswordRequest;
import com.sakura.passage_creator.user.model.entity.User;
import com.sakura.passage_creator.user.model.vo.UserVO;
import com.sakura.passage_creator.user.service.UserService;
import io.github.linpeilie.Converter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端用户接口
 * 作者：Sakura
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Resource
    private UserService userService;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    @Resource
    private Converter converter;

    /**
     * 根据 id 获取用户包装类。
     *
     * @param id 用户 id
     * @param request HTTP 请求
     * @return 用户包装信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(@RequestParam @Positive(message = "用户 id 必须大于 0") long id,
            HttpServletRequest request) {
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户封装列表。
     *
     * @param userQueryRequest 查询参数
     * @param request HTTP 请求
     * @return 分页结果
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@Valid @RequestBody UserQueryRequest userQueryRequest,
            HttpServletRequest request) {
        long current = userQueryRequest.getPage();
        long size = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotalRow());
        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 更新个人信息。
     *
     * @param userUpdateMyRequest 个人信息更新请求
     * @param request HTTP 请求
     * @return 是否更新成功
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@Valid @RequestBody UserUpdateMyRequest userUpdateMyRequest,
            HttpServletRequest request) {
        User loginUser = getCurrentLoginUser();
        User user = converter.convert(userUpdateMyRequest, User.class);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param userUpdatePasswordRequest 修改密码请求
     * @param request HTTP 请求
     * @return 是否修改成功
     */
    @PostMapping("/password/update")
    public BaseResponse<Boolean> updateMyPassword(@Valid @RequestBody UserUpdatePasswordRequest userUpdatePasswordRequest,
            HttpServletRequest request) {
        User loginUser = getCurrentLoginUser();
        boolean result = userService.updateMyPassword(loginUser, userUpdatePasswordRequest.getOldPassword(),
                userUpdatePasswordRequest.getNewPassword(), userUpdatePasswordRequest.getCheckPassword());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 从登录上下文读取当前用户实体。
     *
     * @return 当前登录用户实体
     */
    private User getCurrentLoginUser() {
        LoginUserInfo loginUserInfo = LoginUserContext.getLoginUser();
        ThrowUtils.throwIf(loginUserInfo == null || loginUserInfo.userId() == null, ErrorCode.NOT_LOGIN_ERROR);
        User loginUser = userService.getById(loginUserInfo.userId());
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return loginUser;
    }
}
