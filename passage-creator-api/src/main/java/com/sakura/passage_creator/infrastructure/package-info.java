/**
 * 基础设施模块，负责第三方组件、配置、技术适配和框架集成。
 */
@ApplicationModule(
        displayName = "基础设施模块",
        allowedDependencies = "shared",
        type = ApplicationModule.Type.OPEN
)
package com.sakura.passage_creator.infrastructure;

import org.springframework.modulith.ApplicationModule;
