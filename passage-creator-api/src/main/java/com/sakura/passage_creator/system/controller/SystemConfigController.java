package com.sakura.passage_creator.system.controller;

import com.sakura.passage_creator.shared.enums.AuditOperationTypeEnum;
import com.sakura.passage_creator.shared.annotation.AuditLogRecord;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.system.model.dto.SystemConfigAddRequest;
import com.sakura.passage_creator.system.model.dto.SystemConfigUpdateRequest;
import com.sakura.passage_creator.system.model.vo.SystemConfigVO;
import com.sakura.passage_creator.system.service.SystemConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统配置接口。
 */
@RestController
@RequestMapping("/system/config")
@Validated
public class SystemConfigController {

    /**
     * 系统配置服务。
     */
    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 根据配置键查询系统配置。
     *
     * @param key 配置键
     * @return 系统配置，不存在时返回 null
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SystemConfigVO> getConfigByKey(@RequestParam @NotBlank(message = "配置键不能为空") String key) {
        return ResultUtils.success(systemConfigService.getByKey(key));
    }

    /**
     * 新增系统配置。
     *
     * @param request 新增请求
     * @return 是否成功
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "新增系统配置", module = "系统配置", operationType = AuditOperationTypeEnum.CREATE)
    public BaseResponse<Boolean> addConfig(@Valid @RequestBody SystemConfigAddRequest request) {
        return ResultUtils.success(systemConfigService.addConfig(request));
    }

    /**
     * 更新系统配置。
     *
     * @param request 更新请求
     * @return 是否成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "修改系统配置", module = "系统配置", operationType = AuditOperationTypeEnum.UPDATE)
    public BaseResponse<Boolean> updateConfig(@Valid @RequestBody SystemConfigUpdateRequest request) {
        return ResultUtils.success(systemConfigService.updateConfig(request));
    }
}
