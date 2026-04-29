/**
 * 微信公众号模块，负责微信公众号相关配置和第三方平台集成。
 */
@ApplicationModule(
        displayName = "微信公众号模块",
        allowedDependencies = { "shared", "infrastructure" }
)
package com.sakura.passage_creator.wxmp;

import org.springframework.modulith.ApplicationModule;
