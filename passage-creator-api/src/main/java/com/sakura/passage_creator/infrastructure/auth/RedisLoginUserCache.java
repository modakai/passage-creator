package com.sakura.passage_creator.infrastructure.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的登录用户快照缓存。
 */
@Slf4j
@Component
public class RedisLoginUserCache implements LoginUserCache {

    /**
     * token 相关配置。
     */
    private final TokenProperties tokenProperties;

    /**
     * JSON 序列化工具。
     */
    private final ObjectMapper objectMapper;

    public RedisLoginUserCache(TokenProperties tokenProperties, ObjectMapper objectMapper) {
        this.tokenProperties = tokenProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoginUserInfo get(Long userId) {
        if (userId == null) {
            return null;
        }
        String cacheValue = RedisUtil.getCacheObject(buildKey(userId));
        if (cacheValue == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cacheValue, LoginUserInfo.class);
        } catch (JsonProcessingException e) {
            log.warn("登录用户快照缓存解析失败，已清理缓存，userId={}", userId, e);
            evict(userId);
            return null;
        }
    }

    @Override
    public void put(LoginUserInfo loginUserInfo) {
        if (loginUserInfo == null || loginUserInfo.userId() == null) {
            return;
        }
        try {
            String cacheValue = objectMapper.writeValueAsString(loginUserInfo);
            RedisUtil.setCacheObject(buildKey(loginUserInfo.userId()), cacheValue,
                    Math.toIntExact(tokenProperties.getLoginUserCacheExpireSeconds()), TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("登录用户快照缓存写入失败，userId={}", loginUserInfo.userId(), e);
        }
    }

    @Override
    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        RedisUtil.deleteObject(buildKey(userId));
    }

    /**
     * 构造登录用户快照缓存 key。
     *
     * @param userId 用户 id
     * @return Redis key
     */
    private String buildKey(Long userId) {
        return tokenProperties.getRedisLoginUserKeyPrefix() + userId;
    }
}
