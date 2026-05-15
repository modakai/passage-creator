package com.sakura.passage_creator.prompt.controller;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackSubmitRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptFeedback;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackStatsVO;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackVO;
import com.sakura.passage_creator.prompt.service.PromptFeedbackService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Prompt 反馈接口，包含用户端提交和管理端查询统计。
 */
@RestController
@RequestMapping("/prompt/feedback")
public class PromptFeedbackController {

    /**
     * Prompt 反馈服务。
     */
    private final PromptFeedbackService promptFeedbackService;

    public PromptFeedbackController(PromptFeedbackService promptFeedbackService) {
        this.promptFeedbackService = promptFeedbackService;
    }

    /**
     * 用户端提交或更新当前任务环节反馈。
     */
    @PostMapping("/submit")
    public BaseResponse<PromptFeedbackVO> submitFeedback(@Valid @RequestBody PromptFeedbackSubmitRequest request) {
        return ResultUtils.success(promptFeedbackService.submitFeedback(request, LoginUserContext.getLoginUser()));
    }

    /**
     * 管理端分页查询 Prompt 反馈记录。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PromptFeedbackVO>> listFeedbackByPage(
            @Valid @RequestBody PromptFeedbackQueryRequest request) {
        Page<PromptFeedback> page = promptFeedbackService.page(Page.of(request.getPage(), request.getPageSize()),
                promptFeedbackService.getQueryWrapper(request));
        List<PromptFeedbackVO> voList = promptFeedbackService.getFeedbackVO(page.getRecords());
        Page<PromptFeedbackVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 管理端按已提交反馈统计四档满意度占比。
     */
    @PostMapping("/stats")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<PromptFeedbackStatsVO>> listStats(@Valid @RequestBody PromptFeedbackQueryRequest request) {
        return ResultUtils.success(promptFeedbackService.listStats(request));
    }
}
