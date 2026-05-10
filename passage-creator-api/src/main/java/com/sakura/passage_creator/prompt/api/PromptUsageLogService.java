package com.sakura.passage_creator.prompt.api;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.prompt.model.dto.PromptUsageLogCreateRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptUsageLogQueryRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptUsageLog;
import com.sakura.passage_creator.prompt.model.vo.PromptUsageLogVO;

import java.util.List;

/**
 * Prompt 使用日志服务。
 */
public interface PromptUsageLogService extends IService<PromptUsageLog> {

    /**
     * 记录 Prompt 使用日志。
     *
     * @param request 日志创建命令
     */
    void recordUsage(PromptUsageLogCreateRequest request);

    /**
     * 根据渲染结果记录 Prompt 使用日志。
     *
     * @param result 渲染结果
     * @param agentName Agent 名称
     * @param taskId 任务 id
     * @param responseOk 是否成功
     * @param errorMessage 失败原因
     * @param latencyMs 耗时毫秒
     */
    void recordUsage(PromptTemplateRenderResult result, String agentName, String taskId, boolean responseOk,
            String errorMessage, Integer latencyMs);

    /**
     * 构建分页查询条件。
     *
     * @param request 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(PromptUsageLogQueryRequest request);

    /**
     * 转换为视图对象。
     *
     * @param log 日志实体
     * @return 视图对象
     */
    PromptUsageLogVO getUsageLogVO(PromptUsageLog log);

    /**
     * 批量转换为视图对象。
     *
     * @param logList 日志实体列表
     * @return 视图对象列表
     */
    List<PromptUsageLogVO> getUsageLogVO(List<PromptUsageLog> logList);
}
