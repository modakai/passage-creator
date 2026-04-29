package com.sakura.passage_creator.auth.repository;

import com.sakura.passage_creator.auth.model.entity.OnlineUserSession;

import java.util.Date;
import java.util.List;

/**
 * 在线会话仓储端口。
 *
 * @author Sakura
 */
public interface OnlineUserSessionRepository {

    /**
     * 保存在线会话。
     *
     * @param session 在线会话
     */
    void save(OnlineUserSession session);

    /**
     * 按会话 id 查询在线会话。
     *
     * @param sessionId 会话 id
     * @return 在线会话，不存在时返回 null
     */
    OnlineUserSession findBySessionId(String sessionId);

    /**
     * 按用户 id 查询在线会话。
     *
     * @param userId 用户 id
     * @return 在线会话，不存在时返回 null
     */
    OnlineUserSession findByUserId(Long userId);

    /**
     * 查询全部在线会话并清理失效索引。
     *
     * @return 在线会话列表
     */
    List<OnlineUserSession> listAll();

    /**
     * 刷新最近访问时间。
     *
     * @param userId 用户 id
     * @param lastAccessTime 最近访问时间
     */
    void refreshLastAccess(Long userId, Date lastAccessTime);

    /**
     * 按会话 id 删除在线会话。
     *
     * @param sessionId 会话 id
     */
    void deleteBySessionId(String sessionId);

    /**
     * 按用户 id 删除在线会话。
     *
     * @param userId 用户 id
     */
    void deleteByUserId(Long userId);
}
