package com.sakura.passage_creator.billing.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeCreateRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeQueryRequest;
import com.sakura.passage_creator.billing.model.dto.ManualRechargeReviewRequest;
import com.sakura.passage_creator.billing.model.entity.CreditRechargeApplication;
import com.sakura.passage_creator.billing.model.vo.ManualRechargeApplicationVO;
import com.sakura.passage_creator.billing.model.vo.ManualRechargePackageVO;

import java.util.List;

/**
 * 人工扫码充值申请服务。
 */
public interface ManualRechargeApplicationService extends IService<CreditRechargeApplication> {

    /**
     * 查询当前启用的人工充值套餐。
     */
    List<ManualRechargePackageVO> listPackages();

    /**
     * 创建当前用户的人工充值申请。
     */
    ManualRechargeApplicationVO createApplication(Long userId, ManualRechargeCreateRequest request);

    /**
     * 分页查询当前用户自己的充值申请。
     */
    Page<ManualRechargeApplicationVO> pageMyApplications(ManualRechargeQueryRequest request, Long userId);

    /**
     * 查询当前用户自己的充值申请详情。
     */
    ManualRechargeApplicationVO getMyApplication(Long id, Long userId);

    /**
     * 管理端分页查询所有充值申请。
     */
    Page<ManualRechargeApplicationVO> pageAdminApplications(ManualRechargeQueryRequest request);

    /**
     * 管理员审核通过充值申请。
     */
    ManualRechargeApplicationVO approve(ManualRechargeReviewRequest request, String auditor);

    /**
     * 管理员拒绝充值申请。
     */
    ManualRechargeApplicationVO reject(ManualRechargeReviewRequest request, String auditor);
}
