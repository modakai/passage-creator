/**
 * 文件模块，负责文件上传、OSS 存储访问和文件访问地址生成。
 */
@ApplicationModule(
        displayName = "文件模块",
        allowedDependencies = { "shared", "infrastructure" }
)
package com.sakura.passage_creator.file;

import org.springframework.modulith.ApplicationModule;
