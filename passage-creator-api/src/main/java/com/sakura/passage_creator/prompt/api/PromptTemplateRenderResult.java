package com.sakura.passage_creator.prompt.api;

/**
 * Prompt 运行时渲染结果。
 *
 * @param promptTemplateId 模板版本 id，使用默认兜底模板时为空
 * @param templateKey 模板标识
 * @param version 版本号
 * @param environment 运行环境
 * @param content 渲染后的 Prompt 文本
 * @param fallback 是否来自代码兜底模板
 */
public record PromptTemplateRenderResult(
        Long promptTemplateId,
        String templateKey,
        String version,
        String environment,
        String content,
        boolean fallback
) {
}
