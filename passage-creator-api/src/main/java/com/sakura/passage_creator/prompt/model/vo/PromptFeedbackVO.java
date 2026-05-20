package com.sakura.passage_creator.prompt.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 反馈记录视图对象。
 */
@Data
public class PromptFeedbackVO implements Serializable {

    /**
     * 主键 id。
     */
    private Long id;

    /**
     * 提交反馈的用户 id。
     */
    private Long userId;

    /**
     * 创作任务 id，兼容 article 和 rednote 任务。
     */
    private String taskId;

    /**
     * 反馈环节。
     */
    private String feedbackStage;

    /**
     * 反馈环节展示名称。
     */
    private String feedbackStageLabel;

    /**
     * 满意度结果。
     */
    private String rating;

    /**
     * 满意度结果展示名称。
     */
    private String ratingLabel;

    /**
     * 用户可选填写的文字说明。
     */
    private String remark;

    /**
     * 关联的 Prompt 使用日志 id。
     */
    private Long promptUsageLogId;

    /**
     * 使用的 Prompt 模板版本 id。
     */
    private Long promptTemplateId;

    /**
     * Prompt 模板标识快照。
     */
    private String templateKey;

    /**
     * Prompt 模板版本快照。
     */
    private String version;

    /**
     * Prompt 运行环境快照。
     */
    private String environment;

    /**
     * 是否已关联 Prompt 使用日志。
     */
    private Boolean promptLinked;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
