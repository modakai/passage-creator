package com.sakura.passage_creator.billing.service;

import com.sakura.passage_creator.billing.config.ManualRechargeProperties;
import com.sakura.passage_creator.billing.model.dto.CreditRechargeCommand;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeCreateRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeReviewRequest;
import com.sakura.passage_creator.billing.model.entity.CreditRechargeApplication;
import com.sakura.passage_creator.billing.model.entity.CreditTransaction;
import com.sakura.passage_creator.billing.model.enums.ManualRechargePayMethodEnum;
import com.sakura.passage_creator.billing.model.enums.ManualRechargeStatusEnum;
import com.sakura.passage_creator.billing.service.impl.ManualRechargeApplicationServiceImpl;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人工充值申请服务测试，覆盖套餐可信源和审核幂等这些账务关键规则。
 */
class ManualRechargeApplicationServiceImplTest {

    @Test
    void shouldCreateApplicationUsingConfiguredPackageSnapshot() {
        CreditAccountService creditAccountService = mock(CreditAccountService.class);
        TestableManualRechargeApplicationService service = new TestableManualRechargeApplicationService(
                manualRechargeProperties(), creditAccountService);
        ManualRechargeCreateRequest request = new ManualRechargeCreateRequest();
        request.setPackageId("beta-30");
        request.setPayMethod(ManualRechargePayMethodEnum.ALIPAY.getValue());
        request.setUserRemark("已付款");

        service.createApplication(1001L, request);

        CreditRechargeApplication saved = service.savedApplications.get(0);
        assertEquals("beta-30", saved.getPackageId());
        assertEquals(new BigDecimal("30.00"), saved.getAmount());
        assertEquals(new BigDecimal("350.0000"), saved.getCredits());
        assertEquals(ManualRechargeStatusEnum.PENDING.getValue(), saved.getStatus());
    }

    @Test
    void shouldApprovePendingApplicationAndRechargeCredits() {
        CreditAccountService creditAccountService = mock(CreditAccountService.class);
        when(creditAccountService.recharge(any(CreditRechargeCommand.class), eq("admin"))).thenReturn(new CreditTransaction());
        TestableManualRechargeApplicationService service = new TestableManualRechargeApplicationService(
                manualRechargeProperties(), creditAccountService);
        service.application = pendingApplication();
        ManualRechargeReviewRequest request = new ManualRechargeReviewRequest();
        request.setId(1L);
        request.setAdminRemark("收款已核对");

        service.approve(request, "admin");

        ArgumentCaptor<CreditRechargeCommand> captor = ArgumentCaptor.forClass(CreditRechargeCommand.class);
        verify(creditAccountService).recharge(captor.capture(), eq("admin"));
        CreditRechargeCommand command = captor.getValue();
        assertEquals(1001L, command.getUserId());
        assertEquals(new BigDecimal("350.0000"), command.getAmount());
        assertEquals("MANUAL_RECHARGE", command.getBizType());
        assertEquals("MR202605120001", command.getBizId());
    }

    @Test
    void shouldNotRechargeWhenApproveCalledAgain() {
        CreditAccountService creditAccountService = mock(CreditAccountService.class);
        TestableManualRechargeApplicationService service = new TestableManualRechargeApplicationService(
                manualRechargeProperties(), creditAccountService);
        service.application = pendingApplication();
        service.approveUpdateResult = false;
        ManualRechargeReviewRequest request = new ManualRechargeReviewRequest();
        request.setId(1L);

        assertThrows(BusinessException.class, () -> service.approve(request, "admin"));

        verify(creditAccountService, never()).recharge(any(CreditRechargeCommand.class), eq("admin"));
    }

    @Test
    void shouldNotApproveRejectedApplication() {
        CreditAccountService creditAccountService = mock(CreditAccountService.class);
        TestableManualRechargeApplicationService service = new TestableManualRechargeApplicationService(
                manualRechargeProperties(), creditAccountService);
        CreditRechargeApplication rejected = pendingApplication();
        rejected.setStatus(ManualRechargeStatusEnum.REJECTED.getValue());
        service.application = rejected;
        ManualRechargeReviewRequest request = new ManualRechargeReviewRequest();
        request.setId(1L);

        assertThrows(BusinessException.class, () -> service.approve(request, "admin"));

        verify(creditAccountService, never()).recharge(any(CreditRechargeCommand.class), eq("admin"));
    }

    private ManualRechargeProperties manualRechargeProperties() {
        ManualRechargeProperties properties = new ManualRechargeProperties();
        properties.setWechatQrCodeUrl("/manual-recharge/wechat-qr.svg");
        properties.setAlipayQrCodeUrl("/manual-recharge/alipay-qr.svg");
        ManualRechargeProperties.PackageConfig packageConfig = new ManualRechargeProperties.PackageConfig();
        packageConfig.setPackageId("beta-30");
        packageConfig.setName("30 元套餐");
        packageConfig.setAmount(new BigDecimal("30.00"));
        packageConfig.setCredits(new BigDecimal("350.0000"));
        packageConfig.setEnabled(true);
        packageConfig.setSortOrder(20);
        properties.setPackages(List.of(packageConfig));
        return properties;
    }

    private CreditRechargeApplication pendingApplication() {
        CreditRechargeApplication application = new CreditRechargeApplication();
        application.setId(1L);
        application.setRechargeNo("MR202605120001");
        application.setUserId(1001L);
        application.setPackageId("beta-30");
        application.setAmount(new BigDecimal("30.00"));
        application.setCredits(new BigDecimal("350.0000"));
        application.setPayMethod(ManualRechargePayMethodEnum.WECHAT.getValue());
        application.setStatus(ManualRechargeStatusEnum.PENDING.getValue());
        return application;
    }

    /**
     * 测试替身只替换持久化边界，保留服务真实业务规则。
     */
    private static class TestableManualRechargeApplicationService extends ManualRechargeApplicationServiceImpl {

        private final List<CreditRechargeApplication> savedApplications = new ArrayList<>();

        private CreditRechargeApplication application;

        private boolean approveUpdateResult = true;

        TestableManualRechargeApplicationService(ManualRechargeProperties properties,
                CreditAccountService creditAccountService) {
            super(properties, creditAccountService);
        }

        @Override
        protected boolean saveApplication(CreditRechargeApplication application) {
            savedApplications.add(application);
            return true;
        }

        @Override
        protected CreditRechargeApplication getApplicationById(Long id) {
            return application;
        }

        @Override
        protected boolean markApprovedIfPending(Long id, String adminRemark, String auditor) {
            return approveUpdateResult;
        }

        @Override
        protected <T> T executeInTransaction(Supplier<T> callback) {
            return callback.get();
        }
    }
}
