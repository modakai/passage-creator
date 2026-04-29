package com.sakura.passage_creator.observability.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 错误趋势时间桶。
 *
 * @author Sakura
 */
@Data
public class ErrorTrendBucketVO implements Serializable {

    /**
     * 时间桶标签。
     */
    private String bucket;

    /**
     * 4xx 错误数量。
     */
    private long clientErrorCount;

    /**
     * 5xx 错误数量。
     */
    private long serverErrorCount;

    /**
     * 异常数量。
     */
    private long exceptionCount;

    private static final long serialVersionUID = 1L;
}
