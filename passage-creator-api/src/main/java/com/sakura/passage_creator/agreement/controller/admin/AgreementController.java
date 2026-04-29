package com.sakura.passage_creator.agreement.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.agreement.model.dto.AgreementAddRequest;
import com.sakura.passage_creator.agreement.model.dto.AgreementQueryRequest;
import com.sakura.passage_creator.agreement.model.dto.AgreementUpdateRequest;
import com.sakura.passage_creator.agreement.model.entity.Agreement;
import com.sakura.passage_creator.agreement.model.vo.AgreementVO;
import com.sakura.passage_creator.agreement.service.AgreementService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台协议内容接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/agreement")
@Validated
public class AgreementController {

    @Resource
    private AgreementService agreementService;

    /**
     * 新增协议。
     *
     * @param request 新增请求
     * @param httpServletRequest 当前请求
     * @return 新增记录 id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addAgreement(@Valid @RequestBody AgreementAddRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(agreementService.addAgreement(request));
    }

    /**
     * 更新协议。
     *
     * @param request 更新请求
     * @param httpServletRequest 当前请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAgreement(@Valid @RequestBody AgreementUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(agreementService.updateAgreement(request));
    }

    /**
     * 删除协议。
     *
     * @param deleteRequest 删除请求
     * @param httpServletRequest 当前请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAgreement(@Valid @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(agreementService.removeAgreement(deleteRequest.getId()));
    }

    /**
     * 根据 id 获取协议详情。
     *
     * @param id 协议 id
     * @param httpServletRequest 当前请求
     * @return 协议详情
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AgreementVO> getAgreementById(
            @RequestParam @Positive(message = "协议 id 必须大于 0") long id,
            HttpServletRequest httpServletRequest) {
        Agreement agreement = agreementService.getById(id);
        ThrowUtils.throwIf(agreement == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(agreementService.getAgreementVO(agreement));
    }

    /**
     * 分页获取协议列表。
     *
     * @param queryRequest 查询请求
     * @param httpServletRequest 当前请求
     * @return 分页结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AgreementVO>> listAgreementByPage(@Valid @RequestBody AgreementQueryRequest queryRequest,
            HttpServletRequest httpServletRequest) {
        long current = queryRequest.getPage();
        long pageSize = queryRequest.getPageSize();
        Page<Agreement> page = agreementService.page(new Page<>(current, pageSize),
                agreementService.getQueryWrapper(queryRequest));
        List<AgreementVO> agreementVOList = agreementService.getAgreementVO(page.getRecords());
        Page<AgreementVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(agreementVOList);
        return ResultUtils.success(voPage);
    }
}
