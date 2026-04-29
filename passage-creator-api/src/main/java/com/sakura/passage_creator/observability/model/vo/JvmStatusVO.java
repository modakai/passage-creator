package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * JVM 运行状态。
 *
 * @author Sakura
 */
@Data
public class JvmStatusVO implements Serializable {

    /**
     * 堆内存快照。
     */
    private MetricSnapshotVO heapMemory;

    /**
     * 非堆内存快照。
     */
    private MetricSnapshotVO nonHeapMemory;

    /**
     * 当前线程数。
     */
    private int threadCount;

    /**
     * 守护线程数。
     */
    private int daemonThreadCount;

    /**
     * GC 总次数。
     */
    private long gcCount;

    /**
     * GC 总耗时，单位毫秒。
     */
    private long gcTimeMillis;

    /**
     * JVM 综合状态。
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
