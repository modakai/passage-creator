package com.sakura.passage_creator.observability.service.impl;

import com.sakura.passage_creator.observability.enums.ObservabilityStatusLevelEnum;
import com.sakura.passage_creator.observability.model.vo.MetricSnapshotVO;

import java.lang.management.MemoryUsage;

/**
 * JVM 内存指标构造器，区分堆内存和非堆内存的告警口径。
 *
 * @author Sakura
 */
public class JvmMemoryMetricBuilder {

    /**
     * 字节单位。
     */
    private static final String UNIT_BYTES = "bytes";

    /**
     * 警告阈值。
     */
    private final double warningPercent;

    /**
     * 严重阈值。
     */
    private final double criticalPercent;

    public JvmMemoryMetricBuilder(double warningPercent, double criticalPercent) {
        this.warningPercent = warningPercent;
        this.criticalPercent = criticalPercent;
    }

    /**
     * 构造堆内存指标，堆内存通常有可靠最大值，可以按使用率告警。
     */
    public MetricSnapshotVO buildHeapMemoryMetric(String name, MemoryUsage usage) {
        long total = resolveReliableTotal(usage);
        return buildMetric(name, usage.getUsed(), total, true);
    }

    /**
     * 构造非堆内存指标。非堆由元空间、代码缓存等多块区域组成，聚合 max 容易误导，
     * 因此当前后台页只展示容量信息，不把它纳入异常告警。
     */
    public MetricSnapshotVO buildNonHeapMemoryMetric(String name, MemoryUsage usage) {
        long total = usage.getMax() > 0 ? usage.getMax() : usage.getCommitted();
        return buildMetric(name, usage.getUsed(), total, false);
    }

    /**
     * 构造内存指标。
     */
    private MetricSnapshotVO buildMetric(String name, long used, long total, boolean alertable) {
        double usagePercent = total <= 0 ? 0 : used * 100D / total;
        MetricSnapshotVO vo = new MetricSnapshotVO();
        vo.setName(name);
        vo.setValue(used);
        vo.setUnit(UNIT_BYTES);
        vo.setUsed(used);
        vo.setTotal(total);
        vo.setUsagePercent(usagePercent);
        vo.setStatus(alertable
                ? ObservabilityStatusLevelEnum.fromUsage(usagePercent, warningPercent, criticalPercent).getValue()
                : ObservabilityStatusLevelEnum.UP.getValue());
        return vo;
    }

    /**
     * 获取可靠容量，max 不可用时退回 committed 仅用于展示。
     */
    private long resolveReliableTotal(MemoryUsage usage) {
        return usage.getMax() <= 0 ? usage.getCommitted() : usage.getMax();
    }
}
