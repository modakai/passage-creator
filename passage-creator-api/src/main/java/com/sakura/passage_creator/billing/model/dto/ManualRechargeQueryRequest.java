package com.sakura.passage_creator.billing.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 人工充值申请分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ManualRechargeQueryRequest extends PageRequest implements Serializable {

    /**
     * 管理端可按用户 id 过滤，用户端由服务强制覆盖为当前登录用户。
     */
    private Long userId;

    /**
     * 充值申请状态。
     */
    private String status;

    /**
     * 充值申请号，支持模糊查询。
     */
    private String rechargeNo;

    private static final long serialVersionUID = 1L;
}
