package com.sakura.passage_creator.prompt.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Prompt 反馈提交请求。
 */
@Data
public class PromptFeedbackSubmitRequest implements Serializable {

    /**
     * 文章创作任务 id。
     */
    @NotBlank(message = "任务 id 不能为空")
    private String taskId;

    /**
     * 反馈环节。
     */
    @NotBlank(message = "反馈环节不能为空")
    private String feedbackStage;

    /**
     * 满意度结果。
     */
    @NotBlank(message = "反馈满意度不能为空")
    private String rating;

    /**
     * 保留兼容字段，当前前端不主动提交。
     */
    @Size(max = 1000, message = "反馈说明不能超过 1000 个字符")
    private String remark;

    private static final long serialVersionUID = 1L;
}
