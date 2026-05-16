/**
 * rednote 小红书爆文制作
 *
 * @author sakura
 * @create 2026-05
 */
@ApplicationModule(
        displayName = "rednote 模块",
        allowedDependencies = {"shared", "infrastructure", "audit::api", "prompt::api", "billing::api"}
)
package com.sakura.passage_creator.rednote;

import org.springframework.modulith.ApplicationModule;