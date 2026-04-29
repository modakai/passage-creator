package com.sakura.passage_creator.auth.repository;

import com.sakura.passage_creator.auth.model.entity.OnlineUserSession;
import com.sakura.passage_creator.infrastructure.auth.TokenProperties;
import com.sakura.passage_creator.shared.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的在线会话仓储。
 *
 * @author Sakura
 */
@Repository
@Primary
public class RedisOnlineUserSessionRepository implements OnlineUserSessionRepository {

    /**
     * token 配置。
     */
    private final TokenProperties tokenProperties;

    public RedisOnlineUserSessionRepository(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    @Override
    public void save(OnlineUserSession session) {
        if (session == null || session.getUserId() == null) {
            return;
        }
        session.setSessionId(String.valueOf(session.getUserId()));
        int expireSeconds = Math.toIntExact(tokenProperties.getExpireDurationSeconds());
        RedisUtil.setCacheObject(sessionKey(session.getUserId()), session, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public OnlineUserSession findBySessionId(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        return findByUserId(parseUserId(sessionId));
    }

    @Override
    public OnlineUserSession findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        Object value = RedisUtil.getCacheObject(sessionKey(userId));
        return value instanceof OnlineUserSession session ? session : null;
    }

    @Override
    public List<OnlineUserSession> listAll() {
        Collection<String> tokenKeys = RedisUtil.keys(tokenProperties.getRedisTokenKeyPrefix() + "*");
        List<OnlineUserSession> sessions = new ArrayList<>();
        if (tokenKeys == null || tokenKeys.isEmpty()) {
            return sessions;
        }
        HashSet<Long> userIds = new HashSet<>();
        for (String tokenKey : tokenKeys) {
            Long userId = parseUserId(RedisUtil.getCacheObject(tokenKey));
            if (userId != null) {
                userIds.add(userId);
            }
        }
        for (Long userId : userIds) {
            OnlineUserSession session = findByUserId(userId);
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    @Override
    public void refreshLastAccess(Long userId, Date lastAccessTime) {
        OnlineUserSession session = findByUserId(userId);
        if (session == null) {
            return;
        }
        session.setLastAccessTime(lastAccessTime);
        save(session);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        deleteByUserId(parseUserId(sessionId));
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (userId != null) {
            RedisUtil.deleteObject(sessionKey(userId));
        }
    }

    /**
     * 构造在线会话详情 key，使用用户 id 作为唯一会话标识，避免额外 token->session 映射。
     */
    private String sessionKey(Long userId) {
        return tokenProperties.getRedisOnlineSessionKeyPrefix() + userId;
    }

    /**
     * 将 Redis 中的用户 id 文本转换为 Long，遇到历史脏数据时忽略。
     */
    private Long parseUserId(Object userIdValue) {
        if (userIdValue == null || StringUtils.isBlank(String.valueOf(userIdValue))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(userIdValue));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
