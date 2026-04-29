package com.sakura.passage_creator.audit.api;

/**
 * 审计模块对外 API，供其他模块提交审计事实。
 */
public interface AuditApi {

    /**
     * 提交登录审计日志，审计失败由审计模块内部兜底。
     *
     * @param command 登录审计命令
     */
    void submitLoginLog(LoginAuditCommand command);
}
