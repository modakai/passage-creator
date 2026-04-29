package com.sakura.passage_creator.observability.service.impl;

import com.sakura.passage_creator.observability.config.ObservabilityProperties;
import com.sakura.passage_creator.shared.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败短窗口统计服务，使用 Redis TTL 表达滑动近似窗口。
 *
 * @author Sakura
 */
@Service
public class LoginFailureWindowService {

    /**
     * 登录失败 Redis key 前缀。
     */
    private static final String LOGIN_FAILURE_PREFIX = "observability:login-failure:";

    /**
     * 可观测性配置。
     */
    private final ObservabilityProperties properties;

    public LoginFailureWindowService(ObservabilityProperties properties) {
        this.properties = properties;
    }

    /**
     * 递增 IP 维度失败次数。
     *
     * @param ipAddress IP 地址
     * @return 当前窗口内失败次数
     */
    public long incrementIpFailure(String ipAddress) {
        return incrementWithWindow(buildKey("ip", ipAddress));
    }

    /**
     * 递增账号维度失败次数。
     *
     * @param accountIdentifier 账号标识
     * @return 当前窗口内失败次数
     */
    public long incrementAccountFailure(String accountIdentifier) {
        return incrementWithWindow(buildKey("account", accountIdentifier));
    }

    /**
     * 判断 IP 失败次数是否达到阈值。
     */
    public boolean reachesIpThreshold(long count) {
        return count >= properties.getLoginFailureIpThreshold();
    }

    /**
     * 判断账号失败次数是否达到阈值。
     */
    public boolean reachesAccountThreshold(long count) {
        return count >= properties.getLoginFailureAccountThreshold();
    }

    /**
     * 递增并刷新窗口 TTL。
     */
    private long incrementWithWindow(String key) {
        Long count = RedisUtil.increment(key, 1);
        RedisUtil.expire(key, properties.getLoginFailureWindowSeconds(), TimeUnit.SECONDS);
        return count == null ? 0 : count;
    }

    /**
     * 构造 Redis 统计 key，空值统一归入 unknown，避免 key 为空。
     */
    private String buildKey(String dimension, String value) {
        return LOGIN_FAILURE_PREFIX + dimension + ":" + StringUtils.defaultIfBlank(value, "unknown");
    }
}
