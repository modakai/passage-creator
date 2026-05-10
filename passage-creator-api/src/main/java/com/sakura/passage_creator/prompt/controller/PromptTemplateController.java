package com.sakura.passage_creator.prompt.controller;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateAddRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateRefreshRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateUpdateRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptUsageLogQueryRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptTemplate;
import com.sakura.passage_creator.prompt.model.entity.PromptUsageLog;
import com.sakura.passage_creator.prompt.model.vo.PromptTemplateVO;
import com.sakura.passage_creator.prompt.model.vo.PromptUsageLogVO;
import com.sakura.passage_creator.prompt.api.PromptTemplateService;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.DeleteRequest;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
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
 * 管理端 Prompt 模板接口。
 */
@RestController
@RequestMapping("/prompt/template")
@Validated
public class PromptTemplateController {

    /**
     * Prompt 模板服务。
     */
    private final PromptTemplateService promptTemplateService;

    /**
     * Prompt 使用日志服务。
     */
    private final PromptUsageLogService promptUsageLogService;

    public PromptTemplateController(PromptTemplateService promptTemplateService,
            PromptUsageLogService promptUsageLogService) {
        this.promptTemplateService = promptTemplateService;
        this.promptUsageLogService = promptUsageLogService;
    }

    /**
     * 新增 Prompt 模板草稿。
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addTemplate(@Valid @RequestBody PromptTemplateAddRequest request) {
        return ResultUtils.success(promptTemplateService.addTemplate(request, resolveOperator()));
    }

    /**
     * 更新 Prompt 模板草稿。
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateTemplate(@Valid @RequestBody PromptTemplateUpdateRequest request) {
        return ResultUtils.success(promptTemplateService.updateTemplate(request));
    }

    /**
     * 发布 Prompt 模板版本。
     */
    @PostMapping("/publish")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> publishTemplate(@Valid @RequestBody DeleteRequest request) {
        return ResultUtils.success(promptTemplateService.publishTemplate(request.getId(), resolveOperator()));
    }

    /**
     * 归档 Prompt 模板版本。
     */
    @PostMapping("/archive")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> archiveTemplate(@Valid @RequestBody DeleteRequest request) {
        return ResultUtils.success(promptTemplateService.archiveTemplate(request.getId()));
    }

    /**
     * 删除非 ACTIVE Prompt 模板版本。
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteTemplate(@Valid @RequestBody DeleteRequest request) {
        return ResultUtils.success(promptTemplateService.deleteTemplate(request.getId()));
    }

    /**
     * 查询同模板同环境已经存在的版本号。
     */
    @PostMapping("/versions")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<String>> listTemplateVersions(@Valid @RequestBody PromptTemplateRefreshRequest request) {
        return ResultUtils.success(promptTemplateService.listTemplateVersions(request.getTemplateKey(), request.getEnvironment()));
    }

    /**
     * 主动刷新 Prompt 模板运行时缓存。
     */
    @PostMapping("/refresh")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> refreshTemplate(@Valid @RequestBody PromptTemplateRefreshRequest request) {
        return ResultUtils.success(promptTemplateService.refreshTemplate(request.getTemplateKey(), request.getEnvironment()));
    }

    /**
     * 获取 Prompt 模板详情。
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PromptTemplateVO> getTemplate(@RequestParam @Positive(message = "模板 id 必须大于 0") long id) {
        PromptTemplate template = promptTemplateService.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(promptTemplateService.getTemplateVO(template));
    }

    /**
     * 分页查询 Prompt 模板版本。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PromptTemplateVO>> listTemplateByPage(
            @Valid @RequestBody PromptTemplateQueryRequest request) {
        long current = request.getPage();
        long pageSize = request.getPageSize();
        Page<PromptTemplate> page = promptTemplateService.page(new Page<>(current, pageSize),
                promptTemplateService.getQueryWrapper(request));
        List<PromptTemplateVO> voList = promptTemplateService.getTemplateVO(page.getRecords());
        Page<PromptTemplateVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 分页查询 Prompt 使用日志。
     */
    @PostMapping("/usage/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PromptUsageLogVO>> listUsageLogByPage(
            @Valid @RequestBody PromptUsageLogQueryRequest request) {
        long current = request.getPage();
        long pageSize = request.getPageSize();
        Page<PromptUsageLog> page = promptUsageLogService.page(new Page<>(current, pageSize),
                promptUsageLogService.getQueryWrapper(request));
        List<PromptUsageLogVO> voList = promptUsageLogService.getUsageLogVO(page.getRecords());
        Page<PromptUsageLogVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 解析当前操作人账号。
     */
    private String resolveOperator() {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        if (loginUser == null) {
            return null;
        }
        return loginUser.userAccount();
    }
}
