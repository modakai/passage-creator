/**
 * 审计模块，负责登录审计、操作审计、审计日志查询和审计日志导出。
 */
@ApplicationModule(
        displayName = "审计模块",
        allowedDependencies = "shared"
)
package com.sakura.passage_creator.audit;

import org.springframework.modulith.ApplicationModule;
