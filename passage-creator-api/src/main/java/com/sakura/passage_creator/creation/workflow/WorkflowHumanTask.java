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
 * Workflow 人工交互任务实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("workflow_human_task")
public class WorkflowHumanTask implements Serializable {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.flexId)
    private Long id;

    private String taskId;
    private String bizType;
    private String nodeType;
    private String status;
    private Long assigneeUserId;
    private String inputSnapshotJson;
    private String formSchemaJson;
    private String resultJson;
    private Integer version;
    private LocalDateTime createTime;
    /**
     * 人工任务过期时间，必须和 Graph checkpoint TTL 保持一致。
     */
    private LocalDateTime expireTime;
    private LocalDateTime completedTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
