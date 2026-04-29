package com.sakura.passage_creator.observability.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * 请求观测命令。
 *
 * @author Sakura
 */
@Data
public class RequestObservationCommand {

    /**
     * 请求路径。
     */
    private String requestPath;

    /**
     * HTTP 方法。
     */
    private String httpMethod;

    /**
     * 状态码。
     */
    private Integer statusCode;

    /**
     * 耗时，单位毫秒。
     */
    private Long durationMillis;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 账号标识。
     */
    private String accountIdentifier;

    /**
     * IP 地址。
     */
    private String ipAddress;

    /**
     * 异常对象。
     */
    private Throwable throwable;

    /**
     * 请求发生时间。
     */
    private Date eventTime;
}
