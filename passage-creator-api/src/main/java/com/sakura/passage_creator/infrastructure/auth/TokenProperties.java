package com.sakura.passage_creator.infrastructure.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Token 相关配置。
 *
 * <p>统一管理请求头名称、请求头前缀、有效期、随机串字符源和 Redis 键前缀。</p>
 *
 * 作者：Sakura
 */
@Validated
@Component
@ConfigurationProperties(prefix = "token")
@Setter
@Getter
public class TokenProperties implements InitializingBean {

    /**
     * Token 主请求头名称。
     */
    @NotBlank(message = "token.header-name 不能为空")
    private String headerName = "Authorization";

    /**
     * Token 主请求头前缀。
     */
    private String headerPrefix = "Bearer ";

    /**
     * Token 有效期，单位秒。
     */
    @Min(value = 60, message = "token.expire-duration-seconds 不能小于 60 秒")
    @Max(value = Integer.MAX_VALUE, message = "token.expire-duration-seconds 不能超过 Integer 最大值")
    private long expireDurationSeconds = 30L * 24 * 60 * 60;

    /**
     * Token 随机字符源。
     */
    @NotBlank(message = "token.secret-char-source 不能为空")
    private String secretCharSource =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+-=[]{};:,.?/";

    /**
     * Token 随机长度。
     */
    @Min(value = 16, message = "token.secret-length 不能小于 16")
    private int secretLength = 32;

    /**
     * 是否启用兼容旧请求头。
     */
    private boolean compatibilityHeaderEnabled = true;

    /**
     * 兼容旧 token 请求头名称。
     */
    private String compatibilityHeaderName = "token";

    /**
     * token -> userId 的 Redis key 前缀。
     */
    @NotBlank(message = "token.redis-token-key-prefix 不能为空")
    private String redisTokenKeyPrefix = "login:token:";

    /**
     * userId -> token 的 Redis key 前缀。
     */
    @NotBlank(message = "token.redis-user-key-prefix 不能为空")
    private String redisUserKeyPrefix = "login:user:";

    /**
     * userId -> 登录用户快照的 Redis key 前缀。
     */
    @NotBlank(message = "token.redis-login-user-key-prefix 不能为空")
    private String redisLoginUserKeyPrefix = "login:user-info:";

    /**
     * 登录用户快照缓存有效期，单位秒。
     */
    @Min(value = 60, message = "token.login-user-cache-expire-seconds 不能小于 60 秒")
    @Max(value = Integer.MAX_VALUE, message = "token.login-user-cache-expire-seconds 不能超过 Integer 最大值")
    private long loginUserCacheExpireSeconds = 300L;

    /**
     * 在线会话详情 Redis key 前缀。
     */
    @NotBlank(message = "token.redis-online-session-key-prefix 不能为空")
    private String redisOnlineSessionKeyPrefix = "login:online:session:";

    /**
     * 在线会话最近访问时间最小刷新间隔，单位秒。
     */
    @Min(value = 1, message = "token.online-session-refresh-interval-seconds 不能小于 1 秒")
    @Max(value = Integer.MAX_VALUE, message = "token.online-session-refresh-interval-seconds 不能超过 Integer 最大值")
    private long onlineSessionRefreshIntervalSeconds = 60L;

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /**
     * 校验 token 配置是否合法，防止生成弱 token 或错误 Redis TTL。
     */
    public void validate() {
        if (StringUtils.isBlank(secretCharSource)) {
            throw new IllegalStateException("token.secret-char-source 不能为空");
        }
        if (secretLength < 16) {
            throw new IllegalStateException("token.secret-length 不能小于 16");
        }
        if (expireDurationSeconds < 60 || expireDurationSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("token.expire-duration-seconds 超出允许范围");
        }
        if (loginUserCacheExpireSeconds < 60 || loginUserCacheExpireSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("token.login-user-cache-expire-seconds 超出允许范围");
        }
        if (onlineSessionRefreshIntervalSeconds < 1 || onlineSessionRefreshIntervalSeconds > Integer.MAX_VALUE) {
            throw new IllegalStateException("token.online-session-refresh-interval-seconds 超出允许范围");
        }
        if (compatibilityHeaderEnabled && StringUtils.isBlank(compatibilityHeaderName)) {
            throw new IllegalStateException("启用兼容请求头时，token.compatibility-header-name 不能为空");
        }
    }
}
