package com.sakura.passage_creator.dict.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典映射最小返回对象
 *
 * @author sakura
 */
@Data
public class DictItemSimpleVO implements Serializable {

    /**
     * 标签
     */
    private String label;

    /**
     * 值
     */
    private String value;

    /**
     * 标签样式类型
     */
    private String tagType;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 扩展 JSON
     */
    private String extJson;

    private static final long serialVersionUID = 1L;
}
