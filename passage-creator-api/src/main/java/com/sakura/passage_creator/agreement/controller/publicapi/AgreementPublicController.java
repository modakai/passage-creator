package com.sakura.passage_creator.agreement.controller.publicapi;

import com.sakura.passage_creator.agreement.model.vo.AgreementVO;
import com.sakura.passage_creator.agreement.service.AgreementService;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 协议公开接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/agreement/public")
@Validated
public class AgreementPublicController {

    @Resource
    private AgreementService agreementService;

    /**
     * 根据协议类型获取启用中的协议详情。
     *
     * @param agreementType 协议类型编码
     * @return 协议详情
     */
    @GetMapping("/get")
    public BaseResponse<AgreementVO> getAgreementByType(
            @RequestParam @NotBlank(message = "协议类型不能为空") String agreementType) {
        return ResultUtils.success(agreementService.getAgreementVO(
                agreementService.getEnabledAgreementByType(agreementType)));
    }
}
