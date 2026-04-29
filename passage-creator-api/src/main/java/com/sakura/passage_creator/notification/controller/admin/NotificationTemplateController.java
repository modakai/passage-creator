package com.sakura.passage_creator.notification.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateAddRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateQueryRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateUpdateRequest;
import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import com.sakura.passage_creator.notification.model.vo.NotificationTemplateVO;
import com.sakura.passage_creator.notification.service.NotificationTemplateService;
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
 * 管理端消息通知模板接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/notification/template")
@Validated
public class NotificationTemplateController {

    @Resource
    private NotificationTemplateService templateService;

    /**
     * 新增模板。
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addTemplate(@Valid @RequestBody NotificationTemplateAddRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(templateService.addTemplate(request));
    }

    /**
     * 更新模板。
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateTemplate(@Valid @RequestBody NotificationTemplateUpdateRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(templateService.updateTemplate(request));
    }

    /**
     * 启用模板。
     */
    @PostMapping("/enable")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> enableTemplate(@Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(templateService.enableTemplate(request.getId()));
    }

    /**
     * 停用模板。
     */
    @PostMapping("/disable")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> disableTemplate(@Valid @RequestBody DeleteRequest request,
            HttpServletRequest httpServletRequest) {
        return ResultUtils.success(templateService.disableTemplate(request.getId()));
    }

    /**
     * 获取模板详情。
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<NotificationTemplateVO> getTemplate(@RequestParam @Positive(message = "模板 id 必须大于 0") long id,
            HttpServletRequest httpServletRequest) {
        NotificationTemplate template = templateService.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(templateService.getTemplateVO(template));
    }

    /**
     * 分页查询模板。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<NotificationTemplateVO>> listTemplateByPage(
            @Valid @RequestBody NotificationTemplateQueryRequest request,
            HttpServletRequest httpServletRequest) {
        long current = request.getPage();
        long pageSize = request.getPageSize();
        Page<NotificationTemplate> page = templateService.page(new Page<>(current, pageSize),
                templateService.getQueryWrapper(request));
        List<NotificationTemplateVO> voList = templateService.getTemplateVO(page.getRecords());
        Page<NotificationTemplateVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
