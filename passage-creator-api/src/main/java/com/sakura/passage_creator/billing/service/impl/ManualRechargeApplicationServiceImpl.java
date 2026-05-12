package com.sakura.passage_creator.billing.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.billing.config.ManualRechargeProperties;
import com.sakura.passage_creator.billing.model.dto.CreditRechargeCommand;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeCreateRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeQueryRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeReviewRequest;
import com.sakura.passage_creator.billing.model.entity.CreditRechargeApplication;
import com.sakura.passage_creator.billing.model.enums.ManualRechargePayMethodEnum;
import com.sakura.passage_creator.billing.model.enums.ManualRechargeStatusEnum;
import com.sakura.passage_creator.billing.model.vo.ManualRechargeApplicationVO;
import com.sakura.passage_creator.billing.model.vo.ManualRechargePackageVO;
import com.sakura.passage_creator.billing.model.vo.ManualRechargePaymentVO;
import com.sakura.passage_creator.billing.repository.CreditRechargeApplicationMapper;
import com.sakura.passage_creator.billing.service.AiTokenCostCalculator;
import com.sakura.passage_creator.billing.service.CreditAccountService;
import com.sakura.passage_creator.billing.service.ManualRechargeApplicationService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.sakura.passage_creator.billing.model.entity.table.CreditRechargeApplicationTableDef.CREDIT_RECHARGE_APPLICATION;

/**
 * 人工扫码充值申请服务实现。
 */
@Service
public class ManualRechargeApplicationServiceImpl
        extends ServiceImpl<CreditRechargeApplicationMapper, CreditRechargeApplication>
        implements ManualRechargeApplicationService {

    private static final String MANUAL_RECHARGE_BIZ_TYPE = "MANUAL_RECHARGE";

    private static final DateTimeFormatter RECHARGE_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ManualRechargeProperties properties;

    private final CreditAccountService creditAccountService;

    private final AiTokenCostCalculator amountNormalizer = new AiTokenCostCalculator();

    public ManualRechargeApplicationServiceImpl(ManualRechargeProperties properties,
            CreditAccountService creditAccountService) {
        this.properties = properties;
        this.creditAccountService = creditAccountService;
    }

    @Override
    public List<ManualRechargePackageVO> listPackages() {
        ensureEnabled();
        return activePackages().stream()
                .map(this::toPackageVO)
                .toList();
    }

    @Override
    public ManualRechargeApplicationVO createApplication(Long userId, ManualRechargeCreateRequest request) {
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null || StringUtils.isBlank(request.getPackageId()), ErrorCode.PARAMS_ERROR,
                "套餐不能为空");
        ManualRechargeProperties.PackageConfig packageConfig = findActivePackage(request.getPackageId());
        LocalDateTime now = LocalDateTime.now();
        CreditRechargeApplication application = new CreditRechargeApplication();
        application.setRechargeNo(generateRechargeNo());
        application.setUserId(userId);
        application.setPackageId(packageConfig.getPackageId());
        application.setAmount(normalizeAmount(packageConfig.getAmount()));
        application.setCredits(normalizeCredits(packageConfig.getCredits()));
        application.setPayMethod(ManualRechargePayMethodEnum.of(request.getPayMethod()).getValue());
        application.setStatus(ManualRechargeStatusEnum.PENDING.getValue());
        application.setUserRemark(StringUtils.trimToNull(request.getUserRemark()));
        application.setCreateTime(now);
        application.setUpdateTime(now);
        application.setIsDelete(0);
        saveApplication(application);
        return toVO(application, true);
    }

    @Override
    public Page<ManualRechargeApplicationVO> pageMyApplications(ManualRechargeQueryRequest request, Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        ManualRechargeQueryRequest safeRequest = request == null ? new ManualRechargeQueryRequest() : request;
        safeRequest.setUserId(userId);
        return pageToVO(page(Page.of(safeRequest.getPage(), safeRequest.getPageSize()), getQueryWrapper(safeRequest)),
                true);
    }

    @Override
    public ManualRechargeApplicationVO getMyApplication(Long id, Long userId) {
        ThrowUtils.throwIf(id == null || userId == null, ErrorCode.PARAMS_ERROR);
        CreditRechargeApplication application = getApplicationForUser(id, userId);
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "充值申请不存在");
        return toVO(application, true);
    }

    @Override
    public Page<ManualRechargeApplicationVO> pageAdminApplications(ManualRechargeQueryRequest request) {
        ManualRechargeQueryRequest safeRequest = request == null ? new ManualRechargeQueryRequest() : request;
        return pageToVO(page(Page.of(safeRequest.getPage(), safeRequest.getPageSize()), getQueryWrapper(safeRequest)),
                false);
    }

    @Override
    public ManualRechargeApplicationVO approve(ManualRechargeReviewRequest request, String auditor) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        return executeInTransaction(() -> {
            CreditRechargeApplication application = getApplicationById(request.getId());
            validatePending(application);
            boolean updated = markApprovedIfPending(request.getId(), request.getAdminRemark(), auditor);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "充值申请已被审核，请勿重复操作");
            CreditRechargeCommand command = new CreditRechargeCommand();
            command.setUserId(application.getUserId());
            command.setAmount(application.getCredits());
            command.setBizType(MANUAL_RECHARGE_BIZ_TYPE);
            command.setBizId(application.getRechargeNo());
            command.setDescription("人工充值申请审核通过：" + application.getRechargeNo());
            creditAccountService.recharge(command, auditor);
            CreditRechargeApplication approved = getApplicationById(request.getId());
            return toVO(approved == null ? application : approved, false);
        });
    }

    @Override
    public ManualRechargeApplicationVO reject(ManualRechargeReviewRequest request, String auditor) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isBlank(request.getAdminRemark()), ErrorCode.PARAMS_ERROR, "拒绝原因不能为空");
        return executeInTransaction(() -> {
            CreditRechargeApplication application = getApplicationById(request.getId());
            validatePending(application);
            boolean updated = markRejectedIfPending(request.getId(), request.getAdminRemark(), auditor);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "充值申请已被审核，请勿重复操作");
            CreditRechargeApplication rejected = getApplicationById(request.getId());
            return toVO(rejected == null ? application : rejected, false);
        });
    }

    /**
     * 保存申请，独立方法便于单元测试替换持久化边界。
     */
    protected boolean saveApplication(CreditRechargeApplication application) {
        return save(application);
    }

    /**
     * 按 id 查询申请，独立方法便于测试审核状态机。
     */
    protected CreditRechargeApplication getApplicationById(Long id) {
        return getById(id);
    }

    /**
     * 审核通过时使用状态条件更新，只有抢到 PENDING 状态的请求才能入账。
     */
    protected boolean markApprovedIfPending(Long id, String adminRemark, String auditor) {
        return updateChain()
                .set(CREDIT_RECHARGE_APPLICATION.STATUS, ManualRechargeStatusEnum.APPROVED.getValue())
                .set(CREDIT_RECHARGE_APPLICATION.ADMIN_REMARK, StringUtils.trimToNull(adminRemark))
                .set(CREDIT_RECHARGE_APPLICATION.AUDITOR, StringUtils.defaultIfBlank(auditor, "admin"))
                .set(CREDIT_RECHARGE_APPLICATION.AUDIT_TIME, LocalDateTime.now())
                .set(CREDIT_RECHARGE_APPLICATION.UPDATE_TIME, LocalDateTime.now())
                .where(CREDIT_RECHARGE_APPLICATION.ID.eq(id))
                .and(CREDIT_RECHARGE_APPLICATION.STATUS.eq(ManualRechargeStatusEnum.PENDING.getValue()))
                .update();
    }

    /**
     * 审核拒绝时使用状态条件更新，避免重复修改已审核申请。
     */
    protected boolean markRejectedIfPending(Long id, String adminRemark, String auditor) {
        return updateChain()
                .set(CREDIT_RECHARGE_APPLICATION.STATUS, ManualRechargeStatusEnum.REJECTED.getValue())
                .set(CREDIT_RECHARGE_APPLICATION.ADMIN_REMARK, StringUtils.trimToNull(adminRemark))
                .set(CREDIT_RECHARGE_APPLICATION.AUDITOR, StringUtils.defaultIfBlank(auditor, "admin"))
                .set(CREDIT_RECHARGE_APPLICATION.AUDIT_TIME, LocalDateTime.now())
                .set(CREDIT_RECHARGE_APPLICATION.UPDATE_TIME, LocalDateTime.now())
                .where(CREDIT_RECHARGE_APPLICATION.ID.eq(id))
                .and(CREDIT_RECHARGE_APPLICATION.STATUS.eq(ManualRechargeStatusEnum.PENDING.getValue()))
                .update();
    }

    /**
     * 执行事务，测试中可替换为直接执行。
     */
    protected <T> T executeInTransaction(Supplier<T> callback) {
        final Object[] result = new Object[1];
        Db.tx(() -> {
            result[0] = callback.get();
            return true;
        });
        @SuppressWarnings("unchecked")
        T typedResult = (T) result[0];
        return typedResult;
    }

    private QueryWrapper getQueryWrapper(ManualRechargeQueryRequest request) {
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(CREDIT_RECHARGE_APPLICATION.USER_ID.eq(request.getUserId(), request.getUserId() != null));
        wrapper.and(CREDIT_RECHARGE_APPLICATION.STATUS.eq(request.getStatus(), StringUtils.isNotBlank(request.getStatus())));
        wrapper.and(CREDIT_RECHARGE_APPLICATION.RECHARGE_NO.like(request.getRechargeNo(),
                StringUtils.isNotBlank(request.getRechargeNo())));
        wrapper.orderBy(CREDIT_RECHARGE_APPLICATION.CREATE_TIME, false);
        wrapper.orderBy(CREDIT_RECHARGE_APPLICATION.ID, false);
        return wrapper;
    }

    private CreditRechargeApplication getApplicationForUser(Long id, Long userId) {
        return getOne(QueryWrapper.create()
                .where(CREDIT_RECHARGE_APPLICATION.ID.eq(id))
                .and(CREDIT_RECHARGE_APPLICATION.USER_ID.eq(userId)));
    }

    private void validatePending(CreditRechargeApplication application) {
        ThrowUtils.throwIf(application == null, ErrorCode.NOT_FOUND_ERROR, "充值申请不存在");
        ThrowUtils.throwIf(!ManualRechargeStatusEnum.PENDING.getValue().equals(application.getStatus()),
                ErrorCode.OPERATION_ERROR, "只能审核待审核申请");
    }

    private void ensureEnabled() {
        ThrowUtils.throwIf(!properties.isEnabled(), ErrorCode.OPERATION_ERROR, "人工充值暂未开放");
    }

    private List<ManualRechargeProperties.PackageConfig> activePackages() {
        ensureEnabled();
        ThrowUtils.throwIf(CollUtil.isEmpty(properties.getPackages()), ErrorCode.OPERATION_ERROR, "未配置人工充值套餐");
        return properties.getPackages().stream()
                .filter(ManualRechargeProperties.PackageConfig::isEnabled)
                .sorted(Comparator.comparingInt(ManualRechargeProperties.PackageConfig::getSortOrder))
                .toList();
    }

    private ManualRechargeProperties.PackageConfig findActivePackage(String packageId) {
        return activePackages().stream()
                .filter(item -> StringUtils.equals(item.getPackageId(), packageId))
                .findFirst()
                .orElseThrow(() -> new com.sakura.passage_creator.shared.exception.BusinessException(
                        ErrorCode.PARAMS_ERROR, "套餐不存在或已下架"));
    }

    private String generateRechargeNo() {
        String datePart = LocalDate.now().format(RECHARGE_NO_DATE_FORMATTER);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "MR" + datePart + randomPart;
    }

    private Page<ManualRechargeApplicationVO> pageToVO(Page<CreditRechargeApplication> page, boolean includePayment) {
        List<ManualRechargeApplicationVO> voList = page.getRecords().stream()
                .map(item -> toVO(item, includePayment))
                .toList();
        Page<ManualRechargeApplicationVO> voPage =
                new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }

    private ManualRechargePackageVO toPackageVO(ManualRechargeProperties.PackageConfig config) {
        ManualRechargePackageVO vo = new ManualRechargePackageVO();
        vo.setPackageId(config.getPackageId());
        vo.setName(config.getName());
        vo.setAmount(normalizeAmount(config.getAmount()));
        vo.setCredits(normalizeCredits(config.getCredits()));
        vo.setSortOrder(config.getSortOrder());
        return vo;
    }

    private ManualRechargeApplicationVO toVO(CreditRechargeApplication application, boolean includePayment) {
        if (application == null) {
            return null;
        }
        ManualRechargeApplicationVO vo = new ManualRechargeApplicationVO();
        vo.setId(application.getId());
        vo.setRechargeNo(application.getRechargeNo());
        vo.setUserId(application.getUserId());
        vo.setPackageId(application.getPackageId());
        vo.setAmount(normalizeAmount(application.getAmount()));
        vo.setCredits(normalizeCredits(application.getCredits()));
        vo.setPayMethod(application.getPayMethod());
        vo.setStatus(application.getStatus());
        vo.setUserRemark(application.getUserRemark());
        vo.setAdminRemark(application.getAdminRemark());
        vo.setAuditTime(application.getAuditTime());
        vo.setAuditor(application.getAuditor());
        vo.setCreateTime(application.getCreateTime());
        vo.setUpdateTime(application.getUpdateTime());
        if (includePayment) {
            vo.setPayment(buildPaymentVO(application.getRechargeNo()));
        }
        return vo;
    }

    private ManualRechargePaymentVO buildPaymentVO(String rechargeNo) {
        ManualRechargePaymentVO payment = new ManualRechargePaymentVO();
        payment.setWechatQrCodeUrl(properties.getWechatQrCodeUrl());
        payment.setAlipayQrCodeUrl(properties.getAlipayQrCodeUrl());
        payment.setPaymentRemarkTip("扫码付款后请在付款备注中填写充值申请号：" + StringUtils.defaultString(rechargeNo, "创建后生成"));
        payment.setAuditTip("该方式为人工审核充值，通常需要管理员确认后到账。");
        return payment;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCredits(BigDecimal value) {
        return amountNormalizer.normalize(value);
    }
}
