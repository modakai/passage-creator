package com.sakura.passage_creator.auth.service;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.auth.model.dto.OnlineUserQueryRequest;
import com.sakura.passage_creator.auth.model.vo.OnlineUserVO;
import com.sakura.passage_creator.infrastructure.auth.OnlineSessionTracker;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 在线用户服务。
 *
 * @author Sakura
 */
public interface OnlineUserService extends OnlineSessionTracker {

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
     * 按条件分页查询在线用户。
     *
     * @param request 查询请求
     * @return 在线用户分页
     */
    Page<OnlineUserVO> listOnlineUsers(OnlineUserQueryRequest request);

    /**
     * 强制下线指定在线会话。
     *
     * @param sessionId 目标会话 id
     * @param currentToken 当前请求 token
     * @return 是否下线成功
     */
    boolean forceLogout(String sessionId, String currentToken);
}
