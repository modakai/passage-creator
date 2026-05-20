package com.sakura.passage_creator.rednote.constant;

/**
 * 小红书 Agent Prompt 模板标识和默认用户指令。
 */
public interface RednotePromptConstant {

    /**
     * SearchAgent 系统 Prompt 模板标识。
     */
    String SEARCH_SYSTEM_KEY = "rednote.search.system";

    /**
     * SearchAgent 用户指令模板标识。
     */
    String SEARCH_USER_KEY = "rednote.search.user";

    /**
     * 文案 Agent 系统 Prompt 模板标识。
     */
    String CONTENT_SYSTEM_KEY = "rednote.content.system";

    /**
     * 文案 Agent 用户指令模板标识。
     */
    String CONTENT_USER_KEY = "rednote.content.user";

    /**
     * 普通配图提示词 Agent 系统 Prompt 模板标识。
     */
    String NORMAL_IMAGE_PROMPT_SYSTEM_KEY = "rednote.normal-image-prompt.system";

    /**
     * 普通配图提示词 Agent 用户指令模板标识。
     */
    String NORMAL_IMAGE_PROMPT_USER_KEY = "rednote.normal-image-prompt.user";

    /**
     * 封面图提示词 Agent 系统 Prompt 模板标识。
     */
    String COVER_IMAGE_PROMPT_SYSTEM_KEY = "rednote.cover-image-prompt.system";

    /**
     * 封面图提示词 Agent 用户指令模板标识。
     */
    String COVER_IMAGE_PROMPT_USER_KEY = "rednote.cover-image-prompt.user";

    /**
     * SearchAgent 默认用户指令，保留 {content} 给 Agent 框架运行时替换。
     */
    String SEARCH_USER_PROMPT = "用户需求：{content}";

    /**
     * 文案 Agent 默认用户指令，保留 RednoteBrief 字段占位符给 Agent 框架运行时替换。
     */
    String CONTENT_USER_PROMPT = """
            请基于以下 RednoteBrief 生成小红书结构化文案：
            subject: {subject}
            context: {context}
            contentLength: {contentLength}
            targetWordCount: {targetWordCount}
            keywords: {keywords}
            tagCount: {tagCount}
            searchResults: {searchResults}
            """;

    /**
     * 普通配图提示词 Agent 默认用户指令，保留内容字段占位符给 Agent 框架运行时替换。
     */
    String NORMAL_IMAGE_PROMPT_USER_PROMPT = """
            请基于以下小红书内容生成普通配图提示词：
            subject: {subject}
            bodyContent: {bodyContent}
            tags: {tags}
            imageCount: {imageCount}
            """;

    /**
     * 封面图提示词 Agent 默认用户指令，保留内容字段占位符给 Agent 框架运行时替换。
     */
    String COVER_IMAGE_PROMPT_USER_PROMPT = """
            请基于以下小红书内容生成封面图文案和封面图片提示词：
            subject: {subject}
            bodyContent: {bodyContent}
            tags: {tags}
            """;

    /**
     * SearchAgent 用户指令变量定义。
     */
    String SEARCH_USER_VARIABLE_SCHEMA = """
            [{"name":"content","label":"用户需求","required":true}]
            """;

    /**
     * 文案 Agent 用户指令变量定义。
     */
    String CONTENT_USER_VARIABLE_SCHEMA = """
            [
              {"name":"subject","label":"主题","required":true},
              {"name":"context","label":"创作上下文","required":true},
              {"name":"contentLength","label":"内容长度","required":true},
              {"name":"targetWordCount","label":"目标字数","required":true},
              {"name":"keywords","label":"关键词","required":true},
              {"name":"tagCount","label":"标签数量","required":true},
              {"name":"searchResults","label":"搜索摘要","required":true}
            ]
            """;

    /**
     * 普通配图提示词 Agent 用户指令变量定义。
     */
    String NORMAL_IMAGE_PROMPT_USER_VARIABLE_SCHEMA = """
            [
              {"name":"subject","label":"主题","required":true},
              {"name":"bodyContent","label":"小红书正文","required":true},
              {"name":"tags","label":"标签","required":true},
              {"name":"imageCount","label":"普通配图数量","required":true}
            ]
            """;

    /**
     * 封面图提示词 Agent 用户指令变量定义。
     */
    String COVER_IMAGE_PROMPT_USER_VARIABLE_SCHEMA = """
            [
              {"name":"subject","label":"主题","required":true},
              {"name":"bodyContent","label":"小红书正文","required":true},
              {"name":"tags","label":"标签","required":true}
            ]
            """;
}
