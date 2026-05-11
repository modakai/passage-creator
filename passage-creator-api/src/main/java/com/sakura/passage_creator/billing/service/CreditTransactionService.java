package com.sakura.passage_creator.billing.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.billing.model.dto.CreditTransactionQueryRequest;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.vo.CreditTransactionVO;

import java.util.List;

/**
 * 积分流水服务。
 */
public interface CreditTransactionService extends IService<CreditTransaction> {

    /**
     * 构造积分流水查询条件。
     */
    QueryWrapper getQueryWrapper(CreditTransactionQueryRequest request);

    /**
     * 转换单条流水。
     */
    CreditTransactionVO getTransactionVO(CreditTransaction transaction);

    /**
     * 转换流水列表。
     */
    List<CreditTransactionVO> getTransactionVO(List<CreditTransaction> transactions);
}
