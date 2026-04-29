package com.sakura.passage_creator.auth.service;

import com.sakura.passage_creator.auth.model.vo.LoginUserVO;
import com.sakura.passage_creator.user.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

/**
 * 认证服务。
 */
public interface AuthService {

    /**
     * 用户注册。
     *
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录。
     *
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HTTP 请求
     * @return 登录用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 微信开放平台登录。
     *
     * @param wxOAuth2UserInfo 微信授权用户信息
     * @param request HTTP 请求
     * @return 登录用户信息
     */
    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    /**
     * 获取当前登录用户。
     *
     * @param request HTTP 请求
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户，允许未登录。
     *
     * @param request HTTP 请求
     * @return 当前登录用户，未登录返回 null
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 判断当前请求用户是否为管理员。
     *
     * @param request HTTP 请求
     * @return 是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 判断指定用户是否为管理员。
     *
     * @param user 用户
     * @return 是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 用户注销。
     *
     * @param request HTTP 请求
     * @return 是否注销成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的登录用户信息。
     *
     * @param user 用户实体
     * @return 登录用户视图
     */
    LoginUserVO getLoginUserVO(User user);
}
