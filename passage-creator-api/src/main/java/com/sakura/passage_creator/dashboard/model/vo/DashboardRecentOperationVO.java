package com.sakura.passage_creator.dashboard.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Dashboard 最近操作日志展示项。
 */
@Data
public class DashboardRecentOperationVO implements Serializable {

    /**
     * 审计日志 id。
     */
    private Long id;

    /**
     * 操作人账号或标识。
     */
    private String operator;

    /**
     * 脱敏后的操作动作描述。
     */
    private String action;

    /**
     * 业务模块。
     */
    private String module;

    /**
     * 操作类型。
     */
    private String operationType;

    /**
     * 执行结果。
     */
    private String result;

    /**
     * IP 地址。
     */
    private String ipAddress;

    /**
     * 操作时间。
     */
    private Date operationTime;

    private static final long serialVersionUID = 1L;
}
