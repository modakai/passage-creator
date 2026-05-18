package com.sakura.passage_creator.prompt.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.prompt.model.dto.PromptUsageLogCreateRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptUsageLogQueryRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptUsageLog;
import com.sakura.passage_creator.prompt.model.vo.PromptUsageLogVO;
import com.sakura.passage_creator.prompt.repository.PromptUsageLogMapper;
import com.sakura.passage_creator.prompt.api.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.prompt.model.entity.table.PromptUsageLogTableDef.PROMPT_USAGE_LOG;

/**
 * Prompt 使用日志服务实现。
 */
@Service
@Slf4j
public class PromptUsageLogServiceImpl extends ServiceImpl<PromptUsageLogMapper, PromptUsageLog>
        implements PromptUsageLogService {

    @Override
    public void recordUsage(PromptUsageLogCreateRequest request) {
        if (request == null || StringUtils.isBlank(request.getTemplateKey())) {
            return;
        }
        PromptUsageLog usageLog = new PromptUsageLog();
        usageLog.setPromptTemplateId(request.getPromptTemplateId());
        usageLog.setTemplateKey(request.getTemplateKey());
        usageLog.setVersion(request.getVersion());
        usageLog.setEnvironment(request.getEnvironment());
        usageLog.setAgentName(request.getAgentName());
        usageLog.setTaskId(request.getTaskId());
        usageLog.setSessionId(request.getSessionId());
        usageLog.setUserId(request.getUserId());
        usageLog.setUsedAt(LocalDateTime.now());
        usageLog.setResponseOk(request.getResponseOk());
        usageLog.setErrorMessage(StringUtils.abbreviate(request.getErrorMessage(), 2000));
        usageLog.setLatencyMs(request.getLatencyMs());
        usageLog.setFeedback(request.getFeedback());
        try {
            this.save(usageLog);
        }
        catch (RuntimeException e) {
            log.warn("Prompt 使用日志写入失败，templateKey={}, agentName={}", request.getTemplateKey(), request.getAgentName(), e);
        }
    }

    @Override
    public void recordUsage(PromptTemplateRenderResult result, String agentName, String taskId, boolean responseOk,
            String errorMessage, Integer latencyMs) {
        recordUsage(result, agentName, taskId, resolveCurrentUserId(), responseOk, errorMessage, latencyMs);
    }

    @Override
    public void recordUsage(PromptTemplateRenderResult result, String agentName, String taskId, Long userId,
            boolean responseOk, String errorMessage, Integer latencyMs) {
        if (result == null) {
            return;
        }
        PromptUsageLogCreateRequest request = new PromptUsageLogCreateRequest();
        request.setPromptTemplateId(result.promptTemplateId());
        request.setTemplateKey(result.templateKey());
        request.setVersion(result.version());
        request.setEnvironment(result.environment());
        request.setAgentName(agentName);
        request.setTaskId(taskId);
        request.setUserId(userId);
        request.setResponseOk(responseOk);
        request.setErrorMessage(errorMessage);
        request.setLatencyMs(latencyMs);
        recordUsage(request);
    }

    @Override
    public QueryWrapper getQueryWrapper(PromptUsageLogQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(PROMPT_USAGE_LOG.TEMPLATE_KEY.like(request.getTemplateKey(),
                StringUtils.isNotBlank(request.getTemplateKey())));
        queryWrapper.and(PROMPT_USAGE_LOG.AGENT_NAME.eq(request.getAgentName(),
                StringUtils.isNotBlank(request.getAgentName())));
        queryWrapper.and(PROMPT_USAGE_LOG.TASK_ID.eq(request.getTaskId(), StringUtils.isNotBlank(request.getTaskId())));
        queryWrapper.and(PROMPT_USAGE_LOG.ENVIRONMENT.eq(request.getEnvironment(),
                StringUtils.isNotBlank(request.getEnvironment())));
        queryWrapper.and(PROMPT_USAGE_LOG.RESPONSE_OK.eq(request.getResponseOk(),
                request.getResponseOk() != null));
        queryWrapper.orderBy(PROMPT_USAGE_LOG.USED_AT, false);
        queryWrapper.orderBy(PROMPT_USAGE_LOG.ID, false);
        return queryWrapper;
    }

    @Override
    public PromptUsageLogVO getUsageLogVO(PromptUsageLog log) {
        if (log == null) {
            return null;
        }
        PromptUsageLogVO vo = new PromptUsageLogVO();
        vo.setId(log.getId());
        vo.setPromptTemplateId(log.getPromptTemplateId());
        vo.setTemplateKey(log.getTemplateKey());
        vo.setVersion(log.getVersion());
        vo.setEnvironment(log.getEnvironment());
        vo.setAgentName(log.getAgentName());
        vo.setTaskId(log.getTaskId());
        vo.setUserId(log.getUserId());
        vo.setUsedAt(log.getUsedAt());
        vo.setResponseOk(log.getResponseOk());
        vo.setErrorMessage(log.getErrorMessage());
        vo.setLatencyMs(log.getLatencyMs());
        vo.setFeedback(log.getFeedback());
        return vo;
    }

    @Override
    public List<PromptUsageLogVO> getUsageLogVO(List<PromptUsageLog> logList) {
        if (CollUtil.isEmpty(logList)) {
            return new ArrayList<>();
        }
        return logList.stream().map(this::getUsageLogVO).collect(Collectors.toList());
    }

    /**
     * 从请求上下文读取当前用户 id，异步 Agent 场景不存在上下文时允许为空。
     */
    private Long resolveCurrentUserId() {
        try {
            LoginUserInfo loginUser = LoginUserContext.getLoginUser();
            return loginUser == null ? null : loginUser.userId();
        }
        catch (RuntimeException e) {
            return null;
        }
    }
}
