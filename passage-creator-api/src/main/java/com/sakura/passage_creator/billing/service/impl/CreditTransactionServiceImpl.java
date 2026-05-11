package com.sakura.passage_creator.billing.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.billing.model.dto.CreditTransactionQueryRequest;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.vo.CreditTransactionVO;
import com.sakura.passage_creator.billing.repository.CreditTransactionMapper;
import com.sakura.passage_creator.billing.service.CreditTransactionService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.sakura.passage_creator.billing.model.entity.table.CreditTransactionTableDef.CREDIT_TRANSACTION;

/**
 * 积分流水服务实现。
 */
@Service
public class CreditTransactionServiceImpl extends ServiceImpl<CreditTransactionMapper, CreditTransaction>
        implements CreditTransactionService {

    @Override
    public QueryWrapper getQueryWrapper(CreditTransactionQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(CREDIT_TRANSACTION.USER_ID.eq(request.getUserId(), request.getUserId() != null));
        wrapper.and(CREDIT_TRANSACTION.TRANSACTION_TYPE.eq(request.getTransactionType(),
                StringUtils.isNotBlank(request.getTransactionType())));
        wrapper.and(CREDIT_TRANSACTION.STATUS.eq(request.getStatus(), StringUtils.isNotBlank(request.getStatus())));
        wrapper.and(CREDIT_TRANSACTION.BIZ_TYPE.eq(request.getBizType(), StringUtils.isNotBlank(request.getBizType())));
        wrapper.and(CREDIT_TRANSACTION.BIZ_ID.like(request.getBizId(), StringUtils.isNotBlank(request.getBizId())));
        wrapper.and(CREDIT_TRANSACTION.CREATE_TIME.ge(request.getStartTime(), request.getStartTime() != null));
        wrapper.and(CREDIT_TRANSACTION.CREATE_TIME.le(request.getEndTime(), request.getEndTime() != null));
        wrapper.orderBy(CREDIT_TRANSACTION.CREATE_TIME, false);
        wrapper.orderBy(CREDIT_TRANSACTION.ID, false);
        return wrapper;
    }

    @Override
    public CreditTransactionVO getTransactionVO(CreditTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        CreditTransactionVO vo = new CreditTransactionVO();
        vo.setId(transaction.getId());
        vo.setUserId(transaction.getUserId());
        vo.setTransactionType(transaction.getTransactionType());
        vo.setStatus(transaction.getStatus());
        vo.setAmount(transaction.getAmount());
        vo.setBalanceAfter(transaction.getBalanceAfter());
        vo.setBizType(transaction.getBizType());
        vo.setBizId(transaction.getBizId());
        vo.setDescription(transaction.getDescription());
        vo.setOperator(transaction.getOperator());
        vo.setCreateTime(transaction.getCreateTime());
        return vo;
    }

    @Override
    public List<CreditTransactionVO> getTransactionVO(List<CreditTransaction> transactions) {
        if (CollUtil.isEmpty(transactions)) {
            return new ArrayList<>();
        }
        return transactions.stream().map(this::getTransactionVO).toList();
    }
}
