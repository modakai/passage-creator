package com.sakura.passage_creator.audit.api;

/**
 * 登录审计已提交事件，供观测模块统计登录失败和异常 IP。
 *
 * @param command 登录审计命令
 */
public record LoginAuditSubmittedEvent(LoginAuditCommand command) {
}
