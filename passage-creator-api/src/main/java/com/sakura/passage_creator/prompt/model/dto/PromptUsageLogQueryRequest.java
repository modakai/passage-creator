package com.sakura.passage_creator.prompt.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Prompt 使用日志分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PromptUsageLogQueryRequest extends PageRequest implements Serializable {

    /**
     * 模板标识。
     */
    private String templateKey;

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * 任务 id。
     */
    private String taskId;

    /**
     * 运行环境。
     */
    private String environment;

    /**
     * 是否成功。
     */
    private Boolean responseOk;

    private static final long serialVersionUID = 1L;
}
