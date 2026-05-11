package com.sakura.passage_creator.billing.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.billing.model.dto.AiUsageQueryRequest;
import com.sakura.passage_creator.billing.model.entity.AiUsageRecord;
import com.sakura.passage_creator.billing.model.vo.AiUsageRecordVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageSummaryVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageUserSummaryVO;
import com.sakura.passage_creator.billing.service.AiUsageRecordService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 AI 用量统计接口。
 */
@RestController
@RequestMapping("/ai/usage")
public class AiUsageAdminController {

    private final AiUsageRecordService usageRecordService;

    public AiUsageAdminController(AiUsageRecordService usageRecordService) {
        this.usageRecordService = usageRecordService;
    }

    /**
     * 查询 AI 用量总览。
     */
    @PostMapping("/summary")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AiUsageSummaryVO> getSummary(@Valid @RequestBody AiUsageQueryRequest request) {
        return ResultUtils.success(usageRecordService.getSummary(request));
    }

    /**
     * 按用户聚合查询 AI 用量。
     */
    @PostMapping("/user/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiUsageUserSummaryVO>> listUserSummaryByPage(
            @Valid @RequestBody AiUsageQueryRequest request) {
        return ResultUtils.success(usageRecordService.listUserSummaryByPage(request));
    }

    /**
     * 分页查询 AI 调用明细。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AiUsageRecordVO>> listUsageByPage(@Valid @RequestBody AiUsageQueryRequest request) {
        Page<AiUsageRecord> page = usageRecordService.page(Page.of(request.getPage(), request.getPageSize()),
                usageRecordService.getQueryWrapper(request));
        List<AiUsageRecordVO> voList = usageRecordService.getUsageRecordVO(page.getRecords());
        Page<AiUsageRecordVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
