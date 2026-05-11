package com.sakura.passage_creator.billing.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.billing.model.dto.CreditAccountQueryRequest;
import com.sakura.passage_creator.billing.model.dto.CreditRechargeRequest;
import com.sakura.passage_creator.billing.model.dto.CreditTransactionQueryRequest;
import com.sakura.passage_creator.billing.model.entity.CreditAccount;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.vo.CreditAccountVO;
import com.sakura.passage_creator.billing.model.vo.CreditTransactionVO;
import com.sakura.passage_creator.billing.service.CreditAccountService;
import com.sakura.passage_creator.billing.service.CreditTransactionService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端积分账户接口。
 */
@RestController
@RequestMapping("/credit/admin")
public class CreditAdminController {

    private final CreditAccountService creditAccountService;

    private final CreditTransactionService transactionService;

    public CreditAdminController(CreditAccountService creditAccountService,
            CreditTransactionService transactionService) {
        this.creditAccountService = creditAccountService;
        this.transactionService = transactionService;
    }

    /**
     * 管理员手动给用户充值积分。
     */
    @PostMapping("/recharge")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> recharge(@Valid @RequestBody CreditRechargeRequest request) {
        creditAccountService.recharge(request, resolveOperator());
        return ResultUtils.success(true);
    }

    /**
     * 管理端分页查看用户积分余额。
     */
    @PostMapping("/accounts/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CreditAccountVO>> listAccountsByPage(
            @Valid @RequestBody CreditAccountQueryRequest request) {
        Page<CreditAccount> page = creditAccountService.page(Page.of(request.getPage(), request.getPageSize()),
                creditAccountService.getAccountQueryWrapper(request));
        List<CreditAccountVO> voList = creditAccountService.getAccountVO(page.getRecords());
        Page<CreditAccountVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 管理端分页查看全站积分流水。
     */
    @PostMapping("/transactions/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CreditTransactionVO>> listTransactionsByPage(
            @Valid @RequestBody CreditTransactionQueryRequest request) {
        Page<CreditTransaction> page = transactionService.page(Page.of(request.getPage(), request.getPageSize()),
                transactionService.getQueryWrapper(request));
        List<CreditTransactionVO> voList = transactionService.getTransactionVO(page.getRecords());
        Page<CreditTransactionVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    private String resolveOperator() {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        return loginUser == null ? "admin" : loginUser.userAccount();
    }
}
