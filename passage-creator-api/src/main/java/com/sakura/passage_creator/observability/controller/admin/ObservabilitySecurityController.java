package com.sakura.passage_creator.observability.controller.admin;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.observability.model.dto.ObservabilityEventQueryRequest;
import com.sakura.passage_creator.observability.model.vo.ObservabilityEventVO;
import com.sakura.passage_creator.observability.service.ObservabilityEventService;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端安全事件接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/admin/observability/security")
public class ObservabilitySecurityController {

    /**
     * 运维事件服务。
     */
    private final ObservabilityEventService eventService;

    public ObservabilitySecurityController(ObservabilityEventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 分页查询安全事件。
     */
    @PostMapping("/events/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ObservabilityEventVO>> listSecurityEvents(
            @Valid @RequestBody ObservabilityEventQueryRequest request) {
        return ResultUtils.success(eventService.listSecurityEvents(request));
    }
}
