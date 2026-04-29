package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口质量摘要。
 *
 * @author Sakura
 */
@Data
public class ApiSummaryVO implements Serializable {

    /**
     * 慢接口数量。
     */
    private long slowApiCount;

    /**
     * 错误事件数量。
     */
    private long errorCount;

    /**
     * 平均慢接口耗时。
     */
    private double averageSlowDurationMillis;

    private static final long serialVersionUID = 1L;
}
