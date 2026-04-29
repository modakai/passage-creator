package com.sakura.passage_creator.audit.api;

import java.util.Date;

/**
 * Dashboard 最近操作日志项。
 *
 * @param id 审计日志 id
 * @param operator 操作人
 * @param action 操作动作
 * @param module 业务模块
 * @param operationType 操作类型
 * @param result 执行结果
 * @param ipAddress IP 地址
 * @param operationTime 操作时间
 */
public record DashboardRecentOperation(
        Long id,
        String operator,
        String action,
        String module,
        String operationType,
        String result,
        String ipAddress,
        Date operationTime
) {
}
