/**
 * 用户模块，负责用户资料、登录认证、状态管理和用户侧身份信息。
 */
@ApplicationModule(
        displayName = "用户模块",
        allowedDependencies = { "shared", "infrastructure", "audit::api" },
        type = ApplicationModule.Type.OPEN
)
package com.sakura.passage_creator.user;

import org.springframework.modulith.ApplicationModule;
