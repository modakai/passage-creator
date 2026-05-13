package com.sakura.passage_creator.creation.workflow;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用创作 Workflow 任务实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("workflow_task")
public class WorkflowTask implements Serializable {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    /**
     * 对外任务 ID，和业务任务共享同一个 taskId。
     */
    private String taskId;

    /**
     * 业务类型，例如 article、rednote。
     */
    private String bizType;

    /**
     * 业务主键，例如 article.id。
     */
    private Long bizId;

    /**
     * 任务所属用户。
     */
    private Long userId;

    /**
     * Workflow 状态。
     */
    private String status;

    /**
     * 当前节点类型。
     */
    private String currentNode;

    /**
     * Workflow 上下文 JSON 快照。
     */
    private String contextJson;

    /**
     * 失败原因。
     */
    private String errorMessage;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
