/**
 * 通知模块，负责站内通知、通知模板、通知投递和用户已读状态。
 */
@ApplicationModule(
        displayName = "通知模块",
        allowedDependencies = { "shared", "user::api", "observability::api" }
)
package com.sakura.passage_creator.notification;

import org.springframework.modulith.ApplicationModule;
