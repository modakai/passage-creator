package com.sakura.passage_creator.infrastructure.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 在线会话追踪端口，供基础设施层在登录拦截中刷新在线状态。
 *
 * @author Sakura
 */
public interface OnlineSessionTracker {

    /**
     * 记录登录成功后的在线会话。
     *
     * @param userId 用户 id
     * @param userAccount 用户账号
     * @param userName 用户昵称
     * @param userRole 用户角色
     * @param token 登录 token
     * @param request HTTP 请求
     */
    void recordLoginSession(Long userId, String userAccount, String userName, String userRole, String token,
            HttpServletRequest request);

    /**
     * 刷新在线会话最近访问时间。
     *
     * @param token 登录 token
     */
    void refreshLastAccess(String token);

    /**
     * 按 token 删除在线会话。
     *
     * @param token 登录 token
     */
    void removeByToken(String token);
}
