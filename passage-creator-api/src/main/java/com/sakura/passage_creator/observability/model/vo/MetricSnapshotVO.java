package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 单项指标快照。
 *
 * @author Sakura
 */
@Data
public class MetricSnapshotVO implements Serializable {

    /**
     * 指标名称。
     */
    private String name;

    /**
     * 当前值。
     */
    private double value;

    /**
     * 指标单位。
     */
    private String unit;

    /**
     * 已使用数量。
     */
    private Long used;

    /**
     * 总数量。
     */
    private Long total;

    /**
     * 使用率百分比。
     */
    private Double usagePercent;

    /**
     * 状态等级。
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
