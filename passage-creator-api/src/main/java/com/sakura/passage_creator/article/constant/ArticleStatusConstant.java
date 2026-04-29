package com.sakura.passage_creator.article.constant;

/**
 * 文章状态常量。
 */
public interface ArticleStatusConstant {

    /**
     * 等待处理。
     */
    String PENDING = "PENDING";

    /**
     * 处理中。
     */
    String PROCESSING = "PROCESSING";

    /**
     * 已完成。
     */
    String COMPLETED = "COMPLETED";

    /**
     * 失败。
     */
    String FAILED = "FAILED";
}
