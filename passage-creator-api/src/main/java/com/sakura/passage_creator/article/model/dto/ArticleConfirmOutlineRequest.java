package com.sakura.passage_creator.article.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sakura
 * @create 2026-04
 */
@Data
public class ArticleConfirmOutlineRequest implements Serializable {

    /**
     * 对外任务 ID，使用 UUID 字符串避免暴露自增 id。
     */
    private String taskId;

}
