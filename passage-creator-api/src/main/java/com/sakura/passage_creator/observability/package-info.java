/**
 * 可观测性模块，负责系统状态、接口质量、安全事件和运维告警。
 */
@ApplicationModule(
        displayName = "可观测性模块",
        allowedDependencies = { "shared", "infrastructure", "audit::api" }
)
package com.sakura.passage_creator.observability;

import org.springframework.modulith.ApplicationModule;
