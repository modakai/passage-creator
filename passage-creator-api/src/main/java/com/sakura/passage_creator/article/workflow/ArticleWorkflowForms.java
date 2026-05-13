package com.sakura.passage_creator.article.workflow;

/**
 * 文章 workflow 人工节点表单 schema。
 */
public final class ArticleWorkflowForms {

    private ArticleWorkflowForms() {
    }

    /**
     * 标题确认表单 schema。
     */
    public static String titleConfirmSchema() {
        return """
                {
                  "type": "TITLE_CONFIRM",
                  "fields": [
                    {"name": "selectedMainTitle", "type": "text", "required": true},
                    {"name": "selectedSubTitle", "type": "text", "required": true},
                    {"name": "userDescription", "type": "textarea", "required": false}
                  ]
                }
                """;
    }

    /**
     * 大纲确认表单 schema。
     */
    public static String outlineConfirmSchema() {
        return """
                {
                  "type": "OUTLINE_CONFIRM",
                  "fields": [
                    {"name": "confirmedOutline", "type": "outline-editor", "required": true}
                  ]
                }
                """;
    }
}
