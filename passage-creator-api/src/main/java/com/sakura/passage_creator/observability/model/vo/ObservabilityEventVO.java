package com.sakura.passage_creator.observability.model.vo;

import com.sakura.passage_creator.observability.model.entity.ObservabilityEvent;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 运维观测事件视图。
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = ObservabilityEvent.class)
public class ObservabilityEventVO implements Serializable {

    private Long id;

    private String eventType;

    private String eventLevel;

    private String title;

    private String subject;

    private String requestPath;

    private String httpMethod;

    private Integer statusCode;

    private Long durationMillis;

    private Long userId;

    private String accountIdentifier;

    private String ipAddress;

    private String exceptionSummary;

    private String detail;

    private Long auditLogId;

    private Long notificationId;

    private Date eventTime;

    private static final long serialVersionUID = 1L;
}
