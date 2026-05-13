package com.sakura.passage_creator.article.workflow;

import lombok.Getter;

/**
 * 文章 workflow 节点类型。
 */
@Getter
public enum ArticleWorkflowNodeType {

    TITLE_GENERATING("TITLE_GENERATING"),
    TITLE_CONFIRM("TITLE_CONFIRM"),
    OUTLINE_GENERATING("OUTLINE_GENERATING"),
    OUTLINE_CONFIRM("OUTLINE_CONFIRM"),
    CONTENT_GENERATING("CONTENT_GENERATING"),
    IMAGE_ANALYZING("IMAGE_ANALYZING"),
    IMAGE_GENERATING("IMAGE_GENERATING"),
    CONTENT_MERGING("CONTENT_MERGING");

    private final String value;

    ArticleWorkflowNodeType(String value) {
        this.value = value;
    }
}
