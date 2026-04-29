package com.sakura.passage_creator.observability.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 运维事件查询请求。
 *
 * @author Sakura
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ObservabilityEventQueryRequest extends PageRequest {

    /**
     * 事件类型。
     */
    private String eventType;

    /**
     * 事件级别。
     */
    private String eventLevel;

    /**
     * 请求路径。
     */
    private String requestPath;

    /**
     * IP 地址。
     */
    private String ipAddress;

    /**
     * 账号标识。
     */
    private String accountIdentifier;

    /**
     * 开始时间。
     */
    private Date startTime;

    /**
     * 结束时间。
     */
    private Date endTime;
}
