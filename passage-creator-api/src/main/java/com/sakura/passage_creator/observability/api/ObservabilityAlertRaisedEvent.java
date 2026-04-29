package com.sakura.passage_creator.observability.api;

import java.util.Date;
import java.util.Map;

/**
 * 可观测性告警事件，供通知模块等外部模块监听。
 *
 * @param ruleCode 告警规则编码
 * @param subject 告警主体，例如 IP 或账号
 * @param title 告警标题
 * @param content 告警内容
 * @param level 告警级别
 * @param variables 告警上下文变量
 * @param raisedTime 告警触发时间
 */
public record ObservabilityAlertRaisedEvent(
        String ruleCode,
        String subject,
        String title,
        String content,
        String level,
        Map<String, Object> variables,
        Date raisedTime
) {
}
