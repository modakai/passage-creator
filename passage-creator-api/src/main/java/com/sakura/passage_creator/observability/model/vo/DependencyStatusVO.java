package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 外部依赖状态。
 *
 * @author Sakura
 */
@Data
public class DependencyStatusVO implements Serializable {

    /**
     * 依赖名称。
     */
    private String name;

    /**
     * 状态等级。
     */
    private String status;

    /**
     * 状态说明。
     */
    private String message;

    /**
     * 检查耗时，单位毫秒。
     */
    private Long latencyMillis;

    /**
     * 连接池当前活跃连接数。
     */
    private Integer activeConnections;

    /**
     * 连接池空闲连接数。
     */
    private Integer idleConnections;

    /**
     * 连接池总连接数。
     */
    private Integer totalConnections;

    private static final long serialVersionUID = 1L;
}
