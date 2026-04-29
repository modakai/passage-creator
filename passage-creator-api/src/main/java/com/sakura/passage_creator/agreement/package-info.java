/**
 * 协议模块，负责协议内容、协议类型校验和协议展示相关业务。
 */
@ApplicationModule(
        displayName = "协议模块",
        allowedDependencies = { "shared", "dict::api" }
)
package com.sakura.passage_creator.agreement;

import org.springframework.modulith.ApplicationModule;
