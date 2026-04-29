package com.sakura.passage_creator.infrastructure.auth;

import cn.hutool.core.util.StrUtil;
import com.sakura.passage_creator.shared.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Token 生成与 Redis 登录态管理器。
 *
 * 作者：Sakura
 */
@Component
public class TokenManager {

    /**
     * 安全随机数生成器。
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * token 相关配置。
     */
    private final TokenProperties tokenProperties;

    public TokenManager(TokenProperties tokenProperties) {
        tokenProperties.validate();
        this.tokenProperties = tokenProperties;
    }

    /**
     * 根据配置生成随机 token。
     *
     * @return token 字符串
     */
    public String generateToken() {
        char[] sourceChars = tokenProperties.getSecretCharSource().toCharArray();
        char[] tokenChars = new char[tokenProperties.getSecretLength()];
        for (int i = 0; i < tokenChars.length; i++) {
            tokenChars[i] = randomChar(sourceChars);
        }
        return new String(tokenChars);
    }

    /**
     * 保存 token 和用户 id 的双向映射。
     *
     * @param userId 用户 id
     * @param token token
     */
    public void storeToken(Long userId, String token) {
        String userKey = buildUserKey(userId);
        String oldToken = RedisUtil.getCacheObject(userKey);
        if (StringUtils.isNotBlank(oldToken)) {
            RedisUtil.deleteObject(buildTokenKey(oldToken));
        }
        int expireDurationSeconds = Math.toIntExact(tokenProperties.getExpireDurationSeconds());
        RedisUtil.setCacheObject(buildTokenKey(token), String.valueOf(userId), expireDurationSeconds, TimeUnit.SECONDS);
        RedisUtil.setCacheObject(userKey, token, expireDurationSeconds, TimeUnit.SECONDS);
    }

    /**
     * 根据用户 id 获取当前 token。
     *
     * @param userId 用户 id
     * @return 当前 token，不存在时返回 null
     */
    public String getTokenByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return RedisUtil.getCacheObject(buildUserKey(userId));
    }

    /**
     * 根据 token 获取用户 id。
     *
     * @param token token
     * @return 用户 id，不存在时返回 null
     */
    public Long getUserId(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String userId = RedisUtil.getCacheObject(buildTokenKey(token));
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return Long.valueOf(userId);
    }

    /**
     * 删除登录态。
     *
     * @param token token
     */
    public void removeToken(String token) {
        Long userId = getUserId(token);
        if (userId != null) {
            RedisUtil.deleteObject(buildUserKey(userId));
        }
        if (StringUtils.isNotBlank(token)) {
            RedisUtil.deleteObject(buildTokenKey(token));
        }
    }

    /**
     * 从请求中解析 token。
     *
     * @param request HTTP 请求
     * @return token，不存在时返回 null
     */
    public String resolveToken(HttpServletRequest request) {
        String primaryToken = resolveConfiguredHeaderToken(request);
        if (StringUtils.isNotBlank(primaryToken)) {
            return primaryToken;
        }
        return resolveCompatibilityHeaderToken(request);
    }

    /**
     * 构造 token key。
     *
     * @param token token
     * @return Redis key
     */
    private String buildTokenKey(String token) {
        return tokenProperties.getRedisTokenKeyPrefix() + token;
    }

    /**
     * 构造用户 key。
     *
     * @param userId 用户 id
     * @return Redis key
     */
    private String buildUserKey(Long userId) {
        return tokenProperties.getRedisUserKeyPrefix() + userId;
    }

    /**
     * 从指定字符集中随机取一个字符。
     *
     * @param chars 字符集
     * @return 随机字符
     */
    private char randomChar(char[] chars) {
        return chars[RANDOM.nextInt(chars.length)];
    }

    /**
     * 解析主请求头中的 token。
     *
     * @param request HTTP 请求
     * @return 解析后的 token
     */
    private String resolveConfiguredHeaderToken(HttpServletRequest request) {
        String tokenHeaderValue = request.getHeader(tokenProperties.getHeaderName());
        if (StringUtils.isBlank(tokenHeaderValue)) {
            return null;
        }
        if (StringUtils.isBlank(tokenProperties.getHeaderPrefix())) {
            return StringUtils.trim(tokenHeaderValue);
        }
        if (StringUtils.startsWithIgnoreCase(tokenHeaderValue, tokenProperties.getHeaderPrefix())) {
            // 前缀判断已经忽略大小写，截取时也必须按同一套规则处理，不能再依赖大小写敏感的 substringAfter。
            return StringUtils.trim(tokenHeaderValue.substring(tokenProperties.getHeaderPrefix().length()));
        }
        return null;
    }

    /**
     * 解析兼容请求头中的 token。
     *
     * @param request HTTP 请求
     * @return 解析后的 token
     */
    private String resolveCompatibilityHeaderToken(HttpServletRequest request) {
        if (!tokenProperties.isCompatibilityHeaderEnabled() || StrUtil.isBlank(tokenProperties.getCompatibilityHeaderName())) {
            return null;
        }
        return StringUtils.trimToNull(request.getHeader(tokenProperties.getCompatibilityHeaderName()));
    }
}
