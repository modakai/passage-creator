package com.sakura.passage_creator.prompt.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Prompt 反馈实体，用于记录用户对指定创作环节提示词效果的最终评价。
 */
@Data
@Table("prompt_feedback")
public class PromptFeedback implements Serializable {

    /**
     * 主键 id。
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
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
     * 反馈环节，覆盖文章人工节点和 rednote 终态 Prompt 节点。
     */
    private String feedbackStage;

    /**
     * 满意度结果：VERY_SATISFIED/SATISFIED/NEUTRAL/UNSATISFIED。
     */
    private String rating;

    /**
     * 用户可选填写的文字说明。
     */
    private String remark;

    /**
     * 关联的 Prompt 使用日志 id，无法匹配时为空。
     */
    private Long promptUsageLogId;

    /**
     * 使用的 Prompt 模板版本 id，兜底模板没有数据库记录时为空。
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
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;

    /**
     * 序列化版本号。
     */
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
}
