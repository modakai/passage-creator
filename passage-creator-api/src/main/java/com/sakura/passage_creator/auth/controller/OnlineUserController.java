package com.sakura.passage_creator.auth.controller;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.auth.model.dto.OnlineUserForceLogoutRequest;
import com.sakura.passage_creator.auth.model.dto.OnlineUserQueryRequest;
import com.sakura.passage_creator.auth.model.vo.OnlineUserVO;
import com.sakura.passage_creator.auth.service.OnlineUserService;
import com.sakura.passage_creator.infrastructure.auth.TokenManager;
import com.sakura.passage_creator.shared.annotation.AuditLogRecord;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.enums.AuditOperationTypeEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台在线用户管理接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/online/user")
@Validated
public class OnlineUserController {

    /**
     * 在线用户服务。
     */
    private final OnlineUserService onlineUserService;

    /**
     * Token 管理器。
     */
    private final TokenManager tokenManager;

    public OnlineUserController(OnlineUserService onlineUserService, TokenManager tokenManager) {
        this.onlineUserService = onlineUserService;
        this.tokenManager = tokenManager;
    }

    /**
     * 分页查询在线用户。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<OnlineUserVO>> listOnlineUsers(@Valid @RequestBody OnlineUserQueryRequest request) {
        return ResultUtils.success(onlineUserService.listOnlineUsers(request));
    }

    /**
     * 强制指定在线会话下线。
     */
    @PostMapping("/force-logout")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "强制在线用户下线", module = "在线用户", operationType = AuditOperationTypeEnum.UPDATE)
    public BaseResponse<Boolean> forceLogout(@Valid @RequestBody OnlineUserForceLogoutRequest request,
            HttpServletRequest httpServletRequest) {
        String currentToken = tokenManager.resolveToken(httpServletRequest);
        return ResultUtils.success(onlineUserService.forceLogout(request.getSessionId(), currentToken));
    }
}
