/**
 * Dashboard 模块，负责管理端首页业务统计聚合。
 */
@ApplicationModule(
        displayName = "管理端首页统计模块",
        allowedDependencies = { "shared", "user", "notification::api", "audit::api" }
)
package com.sakura.passage_creator.dashboard;

import org.springframework.modulith.ApplicationModule;
