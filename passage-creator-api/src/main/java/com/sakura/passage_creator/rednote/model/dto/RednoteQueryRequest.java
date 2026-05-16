package com.sakura.passage_creator.rednote.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小红书爆款笔记分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RednoteQueryRequest extends PageRequest implements Serializable {

    /**
     * 小红书笔记 id。
     */
    private Long id;

    /**
     * Workflow 任务 ID。
     */
    private String taskId;

    /**
     * 创建用户 id，管理员可用于后台筛选。
     */
    private Long userId;

    /**
     * 用户原始创作需求关键字。
     */
    private String content;

    /**
     * SearchAgent 解析出的主体关键字。
     */
    private String subject;

    /**
     * 篇幅档位。
     */
    private String contentLength;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 当前生成阶段。
     */
    private String phase;

    /**
     * 创建开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 创建结束时间。
     */
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}
