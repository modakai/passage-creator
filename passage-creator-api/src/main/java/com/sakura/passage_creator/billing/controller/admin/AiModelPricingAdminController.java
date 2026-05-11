package com.sakura.passage_creator.billing.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.billing.model.dto.AiModelPricingQueryRequest;
import com.sakura.passage_creator.billing.model.dto.AiModelPricingSaveRequest;
import com.sakura.passage_creator.billing.model.entity.AiModelPricing;
import com.sakura.passage_creator.billing.model.vo.AiModelPricingVO;
import com.sakura.passage_creator.billing.service.AiModelPricingService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 AI 模型费率配置接口。
 */
@RestController
@RequestMapping("/ai/pricing")
public class AiModelPricingAdminController {

    private final AiModelPricingService pricingService;

    public AiModelPricingAdminController(AiModelPricingService pricingService) {
        this.pricingService = pricingService;
    }

    /**
     * 分页查看模型费率配置。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiModelPricingVO>> listPricingByPage(
            @Valid @RequestBody AiModelPricingQueryRequest request) {
        Page<AiModelPricing> page = pricingService.page(Page.of(request.getPage(), request.getPageSize()),
                pricingService.getQueryWrapper(request));
        List<AiModelPricingVO> voList = pricingService.getPricingVO(page.getRecords());
        Page<AiModelPricingVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 新增或更新模型费率配置。
     */
    @PostMapping("/save")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AiModelPricingVO> savePricing(@Valid @RequestBody AiModelPricingSaveRequest request) {
        AiModelPricing pricing = pricingService.savePricing(request);
        return ResultUtils.success(pricingService.getPricingVO(List.of(pricing)).get(0));
    }

    /**
     * 删除模型费率配置。
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deletePricing(@Valid @RequestBody DeleteRequest request) {
        return ResultUtils.success(pricingService.removeById(request.getId()));
    }
}
