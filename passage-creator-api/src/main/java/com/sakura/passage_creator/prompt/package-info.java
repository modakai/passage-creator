/**
 * Prompt 模块，负责内部 Agent Prompt 模板版本、运行时渲染和使用日志管理。
 */
@ApplicationModule(
        displayName = "Prompt 模块",
        allowedDependencies = { "shared" }
)
package com.sakura.passage_creator.prompt;

import org.springframework.modulith.ApplicationModule;
