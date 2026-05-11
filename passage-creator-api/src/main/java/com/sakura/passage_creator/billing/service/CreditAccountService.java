package com.sakura.passage_creator.billing.service;

import com.mybatisflex.core.service.IService;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.billing.model.dto.CreditAccountQueryRequest;
import com.sakura.passage_creator.billing.model.dto.CreditRechargeRequest;
import com.sakura.passage_creator.billing.model.entity.CreditAccount;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.vo.CreditAccountVO;
import com.sakura.passage_creator.billing.model.vo.CreditSummaryVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 积分账户服务。
 */
public interface CreditAccountService extends IService<CreditAccount> {

    /**
     * 获取或创建用户积分账户。
     */
    CreditAccount ensureAccount(Long userId);

    /**
     * 查询用户积分概览。
     */
    CreditSummaryVO getSummary(Long userId);

    /**
     * 构造管理端用户余额查询条件。
     */
    QueryWrapper getAccountQueryWrapper(CreditAccountQueryRequest request);

    /**
     * 转换用户余额视图。
     */
    List<CreditAccountVO> getAccountVO(List<CreditAccount> records);

    /**
     * 管理员手动充值。
     */
    CreditTransaction recharge(CreditRechargeRequest request, String operator);

    /**
     * AI 调用前预扣积分。
     */
    CreditTransaction reserveCredits(Long userId, BigDecimal amount, String bizType, String bizId, String description);

    /**
     * AI 调用后按真实成本结算预扣流水。
     */
    void settleReserved(Long transactionId, BigDecimal actualCost, String description);

    /**
     * AI 调用失败后释放预扣积分。
     */
    void releaseReserved(Long transactionId, String reason);
}
