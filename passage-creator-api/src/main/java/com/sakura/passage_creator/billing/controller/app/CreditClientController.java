package com.sakura.passage_creator.billing.controller.app;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.billing.model.dto.CreditTransactionQueryRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeCreateRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeQueryRequest;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.vo.ManualRechargeApplicationVO;
import com.sakura.passage_creator.billing.model.vo.ManualRechargePackageVO;
import com.sakura.passage_creator.billing.model.vo.CreditSummaryVO;
import com.sakura.passage_creator.billing.model.vo.CreditTransactionVO;
import com.sakura.passage_creator.billing.service.CreditAccountService;
import com.sakura.passage_creator.billing.service.CreditTransactionService;
import com.sakura.passage_creator.billing.service.ManualRechargeApplicationService;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端积分账户接口。
 */
@RestController
@RequestMapping("/app/credit")
public class CreditClientController {

    private final CreditAccountService creditAccountService;

    private final CreditTransactionService transactionService;

    private final ManualRechargeApplicationService manualRechargeApplicationService;

    public CreditClientController(CreditAccountService creditAccountService,
            CreditTransactionService transactionService,
            ManualRechargeApplicationService manualRechargeApplicationService) {
        this.creditAccountService = creditAccountService;
        this.transactionService = transactionService;
        this.manualRechargeApplicationService = manualRechargeApplicationService;
    }

    /**
     * 查询当前用户积分概览。
     */
    @GetMapping("/summary")
    public BaseResponse<CreditSummaryVO> getSummary() {
        return ResultUtils.success(creditAccountService.getSummary(getLoginUser().userId()));
    }

    /**
     * 当前用户分页查看自己的积分流水。
     */
    @PostMapping("/transactions/page")
    public BaseResponse<Page<CreditTransactionVO>> listMyTransactionsByPage(
            @Valid @RequestBody CreditTransactionQueryRequest request) {
        request.setUserId(getLoginUser().userId());
        Page<CreditTransaction> page = transactionService.page(Page.of(request.getPage(), request.getPageSize()),
                transactionService.getQueryWrapper(request));
        List<CreditTransactionVO> voList = transactionService.getTransactionVO(page.getRecords());
        Page<CreditTransactionVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 查询人工充值可购买套餐。
     */
    @GetMapping("/recharge/packages")
    public BaseResponse<List<ManualRechargePackageVO>> listRechargePackages() {
        return ResultUtils.success(manualRechargeApplicationService.listPackages());
    }

    /**
     * 当前用户创建人工扫码充值申请。
     */
    @PostMapping("/recharge/applications")
    public BaseResponse<ManualRechargeApplicationVO> createRechargeApplication(
            @Valid @RequestBody ManualRechargeCreateRequest request) {
        return ResultUtils.success(manualRechargeApplicationService.createApplication(getLoginUser().userId(), request));
    }

    /**
     * 当前用户分页查看自己的人工充值申请。
     */
    @PostMapping("/recharge/applications/page")
    public BaseResponse<Page<ManualRechargeApplicationVO>> listMyRechargeApplications(
            @Valid @RequestBody ManualRechargeQueryRequest request) {
        return ResultUtils.success(manualRechargeApplicationService.pageMyApplications(request, getLoginUser().userId()));
    }

    /**
     * 当前用户查看自己的人工充值申请详情。
     */
    @GetMapping("/recharge/applications/{id}")
    public BaseResponse<ManualRechargeApplicationVO> getMyRechargeApplication(@PathVariable Long id) {
        return ResultUtils.success(manualRechargeApplicationService.getMyApplication(id, getLoginUser().userId()));
    }

    private LoginUserInfo getLoginUser() {
        return LoginUserContext.getLoginUser();
    }
}
