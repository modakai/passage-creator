package com.sakura.passage_creator.audit.api;

/**
 * 登录审计命令，避免调用方依赖审计模块内部 DTO。
 *
 * @param userId 用户 id
 * @param accountIdentifier 账号标识
 * @param ipAddress IP 地址
 * @param clientInfo 客户端信息
 * @param success 是否成功
 * @param failureReason 失败原因
 * @param costMillis 耗时，单位毫秒
 */
public record LoginAuditCommand(
        Long userId,
        String accountIdentifier,
        String ipAddress,
        String clientInfo,
        boolean success,
        String failureReason,
        long costMillis
) {
}
