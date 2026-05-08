package com.sakura.passage_creator.prompt.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Prompt 变量定义 JSON 归一化器。
 */
@Component
public class PromptTemplateJsonSchemaNormalizer {

    /**
     * MySQL JSON 字段允许 NULL，但不允许空字符串作为空 JSON 文档。
     */
    public String normalize(String variablesSchema) {
        if (StringUtils.isBlank(variablesSchema)) {
            return null;
        }
        return variablesSchema.trim();
    }
}
