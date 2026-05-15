package com.sakura.passage_creator.prompt.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Prompt 反馈满意度占比统计视图对象。
 */
@Data
public class PromptFeedbackStatsVO implements Serializable {

    /**
     * 反馈环节。
     */
    private String feedbackStage;

    /**
     * 反馈环节展示名称。
     */
    private String feedbackStageLabel;

    /**
     * 非常满意数量。
     */
    private long verySatisfiedCount;

    /**
     * 满意数量。
     */
    private long satisfiedCount;

    /**
     * 一般数量。
     */
    private long neutralCount;

    /**
     * 不满意数量。
     */
    private long unsatisfiedCount;

    /**
     * 已提交反馈总数。
     */
    private long totalCount;

    /**
     * 非常满意占比，保留四位小数。
     */
    private BigDecimal verySatisfiedRatio;

    /**
     * 满意占比，保留四位小数。
     */
    private BigDecimal satisfiedRatio;

    /**
     * 一般占比，保留四位小数。
     */
    private BigDecimal neutralRatio;

    /**
     * 不满意占比，保留四位小数。
     */
    private BigDecimal unsatisfiedRatio;

    private static final long serialVersionUID = 1L;
}
