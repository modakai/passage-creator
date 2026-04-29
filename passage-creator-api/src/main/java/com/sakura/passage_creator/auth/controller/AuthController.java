package com.sakura.passage_creator.auth.controller;

import com.sakura.passage_creator.auth.model.dto.UserLoginRequest;
import com.sakura.passage_creator.auth.model.dto.UserRegisterRequest;
import com.sakura.passage_creator.auth.model.vo.LoginUserVO;
import com.sakura.passage_creator.auth.service.AuthService;
import com.sakura.passage_creator.shared.annotation.NoLoginRequired;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.infrastructure.config.WxOpenConfig;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.user.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端认证接口
 * 作者：Sakura
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
public class AuthController {

    /**
     * 认证服务。
     */
    private final AuthService authService;

    /**
     * 微信开放平台配置。
     */
    private final WxOpenConfig wxOpenConfig;

    public AuthController(AuthService authService, WxOpenConfig wxOpenConfig) {
        this.authService = authService;
        this.wxOpenConfig = wxOpenConfig;
    }

    /**
     * 用户注册。
     *
     * @param userRegisterRequest 用户注册请求
     * @return 新用户 id
     */
    @PostMapping("/register")
    @NoLoginRequired
    public BaseResponse<Long> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = authService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录。
     *
     * @param userLoginRequest 用户登录请求
     * @param request HTTP 请求
     * @return 登录用户信息和 token
     */
    @PostMapping("/login")
    @NoLoginRequired
    public BaseResponse<LoginUserVO> userLogin(@Valid @RequestBody UserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = authService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 微信开放平台登录。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param code 微信授权 code
     * @return 登录用户信息和 token
     */
    @GetMapping("/login/wx_open")
    @NoLoginRequired
    public BaseResponse<LoginUserVO> userLoginByWxOpen(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("code") @NotBlank(message = "微信授权 code 不能为空") String code) {
        WxOAuth2AccessToken accessToken;
        try {
            WxMpService wxService = wxOpenConfig.getWxMpService();
            accessToken = wxService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxService.getOAuth2Service().getUserInfo(accessToken, code);
            String unionId = userInfo.getUnionId();
            String mpOpenId = userInfo.getOpenid();
            if (StringUtils.isAnyBlank(unionId, mpOpenId)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，系统错误");
            }
            return ResultUtils.success(authService.userLoginByMpOpen(userInfo, request));
        } catch (Exception e) {
            log.error("userLoginByWxOpen error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，系统错误");
        }
    }

    /**
     * 用户注销。
     *
     * @param request HTTP 请求
     * @return 是否注销成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = authService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户。
     *
     * @param request HTTP 请求
     * @return 当前登录用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = authService.getLoginUser(request);
        return ResultUtils.success(authService.getLoginUserVO(user));
    }
}
