package com.sakura.passage_creator.billing.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreditTransactionQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id，管理端可传，用户端会强制覆盖为当前用户。
     */
    private Long userId;

    /**
     * 流水类型。
     */
    private String transactionType;

    /**
     * 流水状态。
     */
    private String status;

    /**
     * 业务类型。
     */
    private String bizType;

    /**
     * 业务 id。
     */
    private String bizId;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;

    private static final long serialVersionUID = 1L;
}
