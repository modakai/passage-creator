package com.sakura.passage_creator.billing.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户积分账户分页查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreditAccountQueryRequest extends PageRequest implements Serializable {

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 是否只看余额大于 0 的账户。
     */
    private Boolean positiveBalanceOnly;

    private static final long serialVersionUID = 1L;
}
