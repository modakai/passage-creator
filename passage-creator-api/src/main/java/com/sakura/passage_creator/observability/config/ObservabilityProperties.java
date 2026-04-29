package com.sakura.passage_creator.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 可观测性配置项，集中控制阈值、窗口和保留策略。
 *
 * @author Sakura
 */
@Data
@Component
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /**
     * 是否启用请求观测过滤器。
     */
    private boolean requestMonitorEnabled = true;

    /**
     * 慢接口阈值，单位毫秒。
     */
    private long slowApiThresholdMillis = 1000L;

    /**
     * 登录失败统计窗口，单位秒。
     */
    private int loginFailureWindowSeconds = 600;

    /**
     * 单 IP 登录失败告警阈值。
     */
    private long loginFailureIpThreshold = 10;

    /**
     * 单账号登录失败告警阈值。
     */
    private long loginFailureAccountThreshold = 10;

    /**
     * 告警冷却时间，单位秒，避免通知风暴。
     */
    private int alertCooldownSeconds = 1800;

    /**
     * 运维事件保留天数。
     */
    private int eventRetentionDays = 30;

    /**
     * 内存使用率警告阈值。
     */
    private double memoryWarningPercent = 80D;

    /**
     * 内存使用率严重阈值。
     */
    private double memoryCriticalPercent = 90D;

    /**
     * 磁盘使用率警告阈值。
     */
    private double diskWarningPercent = 80D;

    /**
     * 磁盘使用率严重阈值。
     */
    private double diskCriticalPercent = 90D;

    /**
     * CPU 使用率警告阈值。
     */
    private double cpuWarningPercent = 80D;

    /**
     * CPU 使用率严重阈值。
     */
    private double cpuCriticalPercent = 90D;
}
