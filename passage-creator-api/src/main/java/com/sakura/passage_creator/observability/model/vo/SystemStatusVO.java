package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统状态聚合视图。
 *
 * @author Sakura
 */
@Data
public class SystemStatusVO implements Serializable {

    /**
     * 采样时间。
     */
    private Date sampleTime;

    /**
     * 综合状态。
     */
    private String overallStatus;

    /**
     * JVM 状态。
     */
    private JvmStatusVO jvm;

    /**
     * 操作系统状态。
     */
    private OsStatusVO os;

    /**
     * 数据库状态。
     */
    private DependencyStatusVO database;

    /**
     * Redis 状态。
     */
    private DependencyStatusVO redis;

    private static final long serialVersionUID = 1L;
}
