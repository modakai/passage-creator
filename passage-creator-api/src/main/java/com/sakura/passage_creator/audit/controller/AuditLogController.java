package com.sakura.passage_creator.audit.controller;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.shared.enums.AuditOperationTypeEnum;
import com.sakura.passage_creator.audit.model.dto.AuditLogExportRequest;
import com.sakura.passage_creator.audit.model.dto.AuditLogQueryRequest;
import com.sakura.passage_creator.audit.model.entity.AuditLog;
import com.sakura.passage_creator.audit.model.excel.AuditLogExcelRow;
import com.sakura.passage_creator.audit.model.vo.AuditLogVO;
import com.sakura.passage_creator.audit.service.AuditLogService;
import com.sakura.passage_creator.shared.annotation.AuditLogRecord;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import com.sakura.passage_creator.shared.util.ExcelUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台审计日志接口。
 *
 * @author Sakura
 */
@RestController
@RequestMapping("/audit/log")
@Validated
public class AuditLogController {

    /**
     * 审计日志服务。
     */
    @Resource
    private AuditLogService auditLogService;

    /**
     * 分页查询审计日志。
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AuditLogVO>> listAuditLogByPage(@Valid @RequestBody AuditLogQueryRequest request,
            HttpServletRequest httpServletRequest) {
        long current = request.getPage();
        long pageSize = request.getPageSize();
        Page<AuditLog> page = auditLogService.page(new Page<>(current, pageSize), auditLogService.getQueryWrapper(request));
        List<AuditLogVO> voList = auditLogService.getAuditLogVO(page.getRecords());
        Page<AuditLogVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 获取审计日志详情。
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AuditLogVO> getAuditLog(@RequestParam @Positive(message = "审计日志 id 必须大于 0") long id,
            HttpServletRequest httpServletRequest) {
        AuditLog auditLog = auditLogService.getById(id);
        ThrowUtils.throwIf(auditLog == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(auditLogService.getAuditLogVO(auditLog));
    }

    /**
     * 导出审计日志 Excel。
     */
    @PostMapping("/export")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @AuditLogRecord(description = "导出审计日志", module = "审计日志", operationType = AuditOperationTypeEnum.EXPORT)
    public ResponseEntity<byte[]> exportAuditLog(@Valid @RequestBody AuditLogExportRequest request,
            HttpServletRequest httpServletRequest) {
        List<AuditLogVO> logs = auditLogService.listExportLogs(request);
        return ExcelUtils.write("audit-logs", "审计日志", AuditLogExcelRow.class, AuditLogExcelRow.fromList(logs));
    }
}
