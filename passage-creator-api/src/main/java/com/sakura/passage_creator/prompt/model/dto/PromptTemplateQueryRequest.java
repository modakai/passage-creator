package com.sakura.passage_creator.prompt.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Prompt 模板分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PromptTemplateQueryRequest extends PageRequest implements Serializable {

    /**
     * 模板 id。
     */
    private Long id;

    /**
     * 模板标识。
     */
    private String templateKey;

    /**
     * 版本号。
     */
    private String version;

    /**
     * 状态。
     */
    private String status;

    /**
     * 运行环境。
     */
    private String environment;

    private static final long serialVersionUID = 1L;
}
