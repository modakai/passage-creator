/**
 * 账本模块，负责 AI 用量统计、积分账户和积分流水。
 */
@ApplicationModule(
        displayName = "账本模块",
        allowedDependencies = { "shared" }
)
package com.sakura.passage_creator.billing;

import org.springframework.modulith.ApplicationModule;
