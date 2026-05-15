package com.sakura.passage_creator.prompt.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 反馈查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PromptFeedbackQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 创作任务 id。
     */
    private String taskId;

    /**
     * 反馈环节。
     */
    private String feedbackStage;

    /**
     * 满意度结果。
     */
    private String rating;

    /**
     * Prompt 模板标识。
     */
    private String templateKey;

    /**
     * Prompt 模板版本。
     */
    private String version;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}
