/**
 * 认证模块，负责登录、注册、注销、当前登录用户和在线用户管理。
 */
@ApplicationModule(
        displayName = "认证模块",
        allowedDependencies = { "shared", "infrastructure", "user", "audit::api" }
)
package com.sakura.passage_creator.auth;

import org.springframework.modulith.ApplicationModule;
