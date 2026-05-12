package com.sakura.passage_creator.billing.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.billing.model.dto.CreditAccountQueryRequest;
import com.sakura.passage_creator.billing.model.dto.CreditRechargeCommand;
import com.sakura.passage_creator.billing.model.dto.CreditRechargeRequest;
import com.sakura.passage_creator.billing.model.entity.CreditAccount;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.enums.CreditTransactionStatusEnum;
import com.sakura.passage_creator.billing.model.enums.CreditTransactionTypeEnum;
import com.sakura.passage_creator.billing.model.vo.CreditAccountVO;
import com.sakura.passage_creator.billing.model.vo.CreditSummaryVO;
import com.sakura.passage_creator.billing.repository.CreditAccountMapper;
import com.sakura.passage_creator.billing.service.AiTokenCostCalculator;
import com.sakura.passage_creator.billing.service.CreditAccountService;
import com.sakura.passage_creator.billing.service.CreditLedgerCalculator;
import com.sakura.passage_creator.billing.service.CreditLedgerSettlement;
import com.sakura.passage_creator.billing.service.CreditTransactionService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.sakura.passage_creator.billing.model.entity.table.CreditAccountTableDef.CREDIT_ACCOUNT;

/**
 * 积分账户服务实现。
 */
@Service
public class CreditAccountServiceImpl extends ServiceImpl<CreditAccountMapper, CreditAccount>
        implements CreditAccountService {

    private static final String SYSTEM_OPERATOR = "SYSTEM";

    private final CreditTransactionService transactionService;

    private final AiTokenCostCalculator amountNormalizer = new AiTokenCostCalculator();

    private final CreditLedgerCalculator ledgerCalculator = new CreditLedgerCalculator();

    public CreditAccountServiceImpl(CreditTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public CreditAccount ensureAccount(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户 id 不能为空");
        CreditAccount account = this.getOne(QueryWrapper.create().where(CREDIT_ACCOUNT.USER_ID.eq(userId)));
        if (account != null) {
            return account;
        }
        CreditAccount newAccount = new CreditAccount();
        newAccount.setUserId(userId);
        newAccount.setBalance(BigDecimal.ZERO.setScale(4));
        newAccount.setTotalRecharge(BigDecimal.ZERO.setScale(4));
        newAccount.setTotalConsume(BigDecimal.ZERO.setScale(4));
        newAccount.setCreateTime(LocalDateTime.now());
        newAccount.setUpdateTime(LocalDateTime.now());
        newAccount.setIsDelete(0);
        this.save(newAccount);
        return newAccount;
    }

    @Override
    public CreditSummaryVO getSummary(Long userId) {
        CreditAccount account = ensureAccount(userId);
        CreditSummaryVO vo = new CreditSummaryVO();
        vo.setUserId(account.getUserId());
        vo.setBalance(normalize(account.getBalance()));
        vo.setTotalRecharge(normalize(account.getTotalRecharge()));
        vo.setTotalConsume(normalize(account.getTotalConsume()));
        return vo;
    }

    @Override
    public QueryWrapper getAccountQueryWrapper(CreditAccountQueryRequest request) {
        CreditAccountQueryRequest safeRequest = request == null ? new CreditAccountQueryRequest() : request;
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(CREDIT_ACCOUNT.USER_ID.eq(safeRequest.getUserId(), safeRequest.getUserId() != null));
        if (Boolean.TRUE.equals(safeRequest.getPositiveBalanceOnly())) {
            wrapper.and(CREDIT_ACCOUNT.BALANCE.gt(BigDecimal.ZERO));
        }
        wrapper.orderBy(CREDIT_ACCOUNT.UPDATE_TIME, false);
        wrapper.orderBy(CREDIT_ACCOUNT.ID, false);
        return wrapper;
    }

    @Override
    public List<CreditAccountVO> getAccountVO(List<CreditAccount> records) {
        return records.stream().map(this::toVO).toList();
    }

    @Override
    public CreditTransaction recharge(CreditRechargeRequest request, String operator) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        CreditRechargeCommand command = new CreditRechargeCommand();
        command.setUserId(request.getUserId());
        command.setAmount(request.getAmount());
        command.setBizType("ADMIN_RECHARGE");
        command.setBizId("manual:" + System.currentTimeMillis());
        command.setDescription(StringUtils.defaultIfBlank(request.getDescription(), "管理员手动充值"));
        return recharge(command, operator);
    }

    @Override
    public CreditTransaction recharge(CreditRechargeCommand command, String operator) {
        ThrowUtils.throwIf(command == null || command.getUserId() == null, ErrorCode.PARAMS_ERROR);
        BigDecimal amount = normalize(command.getAmount());
        ThrowUtils.throwIf(amount.compareTo(BigDecimal.ZERO) <= 0, ErrorCode.PARAMS_ERROR, "充值积分必须大于 0");
        final CreditTransaction[] result = new CreditTransaction[1];
        Db.tx(() -> {
            CreditAccount account = ensureAccount(command.getUserId());
            account.setBalance(normalize(account.getBalance().add(amount)));
            account.setTotalRecharge(normalize(account.getTotalRecharge().add(amount)));
            account.setUpdateTime(LocalDateTime.now());
            this.updateById(account);
            result[0] = saveTransaction(account, CreditTransactionTypeEnum.RECHARGE.getValue(),
                    CreditTransactionStatusEnum.COMPLETED.getValue(), amount, account.getBalance(),
                    StringUtils.defaultIfBlank(command.getBizType(), "ADMIN_RECHARGE"),
                    StringUtils.defaultIfBlank(command.getBizId(), "manual:" + System.currentTimeMillis()),
                    StringUtils.defaultIfBlank(command.getDescription(), "管理员手动充值"), operator);
            return true;
        });
        return result[0];
    }

    @Override
    public CreditTransaction reserveCredits(Long userId, BigDecimal amount, String bizType, String bizId,
            String description) {
        BigDecimal reserveAmount = normalize(amount);
        ThrowUtils.throwIf(reserveAmount.compareTo(BigDecimal.ZERO) <= 0, ErrorCode.PARAMS_ERROR, "预扣积分必须大于 0");
        final CreditTransaction[] result = new CreditTransaction[1];
        Db.tx(() -> {
            CreditAccount account = ensureAccount(userId);
            ThrowUtils.throwIf(normalize(account.getBalance()).compareTo(reserveAmount) < 0,
                    ErrorCode.OPERATION_ERROR, "积分余额不足，请先充值");
            account.setBalance(normalize(account.getBalance().subtract(reserveAmount)));
            account.setUpdateTime(LocalDateTime.now());
            this.updateById(account);
            result[0] = saveTransaction(account, CreditTransactionTypeEnum.RESERVE.getValue(),
                    CreditTransactionStatusEnum.RESERVED.getValue(), reserveAmount, account.getBalance(), bizType,
                    bizId, description, SYSTEM_OPERATOR);
            return true;
        });
        return result[0];
    }

    @Override
    public void settleReserved(Long transactionId, BigDecimal actualCost, String description) {
        if (transactionId == null) {
            return;
        }
        Db.tx(() -> {
            CreditTransaction reserved = transactionService.getById(transactionId);
            if (reserved == null || !CreditTransactionStatusEnum.RESERVED.getValue().equals(reserved.getStatus())) {
                return true;
            }
            CreditAccount account = ensureAccount(reserved.getUserId());
            BigDecimal cost = normalize(actualCost);
            CreditLedgerSettlement settlement = ledgerCalculator.settleReserved(account.getBalance(),
                    reserved.getAmount(), cost);
            ThrowUtils.throwIf(settlement.balanceAfter().compareTo(BigDecimal.ZERO) < 0,
                    ErrorCode.OPERATION_ERROR, "积分余额不足，无法完成本次 AI 成本结算");
            account.setBalance(settlement.balanceAfter());
            account.setTotalConsume(normalize(account.getTotalConsume().add(cost)));
            account.setUpdateTime(LocalDateTime.now());
            this.updateById(account);
            reserved.setStatus(CreditTransactionStatusEnum.COMPLETED.getValue());
            reserved.setDescription(StringUtils.defaultIfBlank(description, reserved.getDescription()));
            reserved.setUpdateTime(LocalDateTime.now());
            transactionService.updateById(reserved);
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                saveTransaction(account, CreditTransactionTypeEnum.CONSUME.getValue(),
                        CreditTransactionStatusEnum.COMPLETED.getValue(), cost, account.getBalance(),
                        reserved.getBizType(), reserved.getBizId(), description, SYSTEM_OPERATOR);
            }
            if (settlement.refundAmount().compareTo(BigDecimal.ZERO) > 0) {
                saveTransaction(account, CreditTransactionTypeEnum.REFUND.getValue(),
                        CreditTransactionStatusEnum.COMPLETED.getValue(), settlement.refundAmount(),
                        account.getBalance(), reserved.getBizType(), reserved.getBizId(), "AI 预扣积分退回",
                        SYSTEM_OPERATOR);
            }
            return true;
        });
    }

    @Override
    public void releaseReserved(Long transactionId, String reason) {
        if (transactionId == null) {
            return;
        }
        Db.tx(() -> {
            CreditTransaction reserved = transactionService.getById(transactionId);
            if (reserved == null || !CreditTransactionStatusEnum.RESERVED.getValue().equals(reserved.getStatus())) {
                return true;
            }
            CreditAccount account = ensureAccount(reserved.getUserId());
            account.setBalance(normalize(account.getBalance().add(reserved.getAmount())));
            account.setUpdateTime(LocalDateTime.now());
            this.updateById(account);
            reserved.setStatus(CreditTransactionStatusEnum.RELEASED.getValue());
            reserved.setDescription(StringUtils.defaultIfBlank(reason, "AI 调用失败，释放预扣积分"));
            reserved.setUpdateTime(LocalDateTime.now());
            transactionService.updateById(reserved);
            saveTransaction(account, CreditTransactionTypeEnum.REFUND.getValue(),
                    CreditTransactionStatusEnum.COMPLETED.getValue(), reserved.getAmount(), account.getBalance(),
                    reserved.getBizType(), reserved.getBizId(), "释放 AI 预扣积分", SYSTEM_OPERATOR);
            return true;
        });
    }

    private CreditTransaction saveTransaction(CreditAccount account, String type, String status, BigDecimal amount,
            BigDecimal balanceAfter, String bizType, String bizId, String description, String operator) {
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUserId(account.getUserId());
        transaction.setAccountId(account.getId());
        transaction.setTransactionType(type);
        transaction.setStatus(status);
        transaction.setAmount(normalize(amount));
        transaction.setBalanceAfter(normalize(balanceAfter));
        transaction.setBizType(bizType);
        transaction.setBizId(bizId);
        transaction.setDescription(description);
        transaction.setOperator(StringUtils.defaultIfBlank(operator, SYSTEM_OPERATOR));
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transaction.setIsDelete(0);
        transactionService.save(transaction);
        return transaction;
    }

    private BigDecimal normalize(BigDecimal value) {
        return amountNormalizer.normalize(value);
    }

    private CreditAccountVO toVO(CreditAccount account) {
        CreditAccountVO vo = new CreditAccountVO();
        vo.setId(account.getId());
        vo.setUserId(account.getUserId());
        vo.setBalance(normalize(account.getBalance()));
        vo.setTotalRecharge(normalize(account.getTotalRecharge()));
        vo.setTotalConsume(normalize(account.getTotalConsume()));
        vo.setCreateTime(account.getCreateTime());
        vo.setUpdateTime(account.getUpdateTime());
        return vo;
    }
}
