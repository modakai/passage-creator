/**
 * 文章模块，负责文章的创建
 */
@ApplicationModule(
        displayName = "文章模块",
        allowedDependencies = {"shared", "infrastructure", "audit::api", "prompt::api"}
)
package com.sakura.passage_creator.article;

import org.springframework.modulith.ApplicationModule;
