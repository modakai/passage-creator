package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作系统资源状态。
 *
 * @author Sakura
 */
@Data
public class OsStatusVO implements Serializable {

    /**
     * 系统 CPU 使用率。
     */
    private MetricSnapshotVO systemCpu;

    /**
     * 当前进程 CPU 使用率。
     */
    private MetricSnapshotVO processCpu;

    /**
     * 系统内存使用情况。
     */
    private MetricSnapshotVO memory;

    /**
     * 磁盘使用情况。
     */
    private MetricSnapshotVO disk;

    /**
     * 操作系统综合状态。
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
