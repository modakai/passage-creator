package com.sakura.passage_creator.billing.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.billing.model.dto.AiUsageQueryRequest;
import com.sakura.passage_creator.billing.model.dto.RecordAiUsageCommand;
import com.sakura.passage_creator.billing.model.entity.AiUsageRecord;
import com.sakura.passage_creator.billing.model.vo.AiUsageRecordVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageSummaryItemVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageSummaryVO;
import com.sakura.passage_creator.billing.model.vo.AiUsageUserSummaryVO;
import com.sakura.passage_creator.billing.repository.AiUsageRecordMapper;
import com.sakura.passage_creator.billing.service.AiTokenCostCalculator;
import com.sakura.passage_creator.billing.service.AiUsageRecordService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.billing.model.entity.table.AiUsageRecordTableDef.AI_USAGE_RECORD;

/**
 * AI 用量记录服务实现。
 */
@Service
@Slf4j
public class AiUsageRecordServiceImpl extends ServiceImpl<AiUsageRecordMapper, AiUsageRecord>
        implements AiUsageRecordService {

    private final AiTokenCostCalculator amountNormalizer = new AiTokenCostCalculator();

    @Override
    public void recordUsage(RecordAiUsageCommand command) {
        if (command == null || command.getUserId() == null) {
            return;
        }
        AiUsageRecord record = new AiUsageRecord();
        record.setUserId(command.getUserId());
        record.setTaskId(command.getTaskId());
        record.setAgentName(command.getAgentName());
        record.setPhase(command.getPhase());
        record.setProvider(command.getProvider());
        record.setModel(command.getModel());
        record.setRequestType(command.getRequestType());
        record.setPromptTokens(safeToken(command.getPromptTokens()));
        record.setCompletionTokens(safeToken(command.getCompletionTokens()));
        record.setTotalTokens(resolveTotalTokens(command));
        record.setCreditCost(amountNormalizer.normalize(command.getCreditCost()));
        record.setLatencyMs(command.getLatencyMs());
        record.setResponseOk(command.getResponseOk());
        record.setErrorMessage(StringUtils.abbreviate(command.getErrorMessage(), 2000));
        record.setUsedAt(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        record.setIsDelete(0);
        try {
            this.save(record);
        }
        catch (RuntimeException e) {
            log.warn("AI 用量记录写入失败, taskId={}, agentName={}", command.getTaskId(), command.getAgentName(), e);
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(AiUsageQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(AI_USAGE_RECORD.USER_ID.eq(request.getUserId(), request.getUserId() != null));
        wrapper.and(AI_USAGE_RECORD.TASK_ID.eq(request.getTaskId(), StringUtils.isNotBlank(request.getTaskId())));
        wrapper.and(AI_USAGE_RECORD.AGENT_NAME.eq(request.getAgentName(), StringUtils.isNotBlank(request.getAgentName())));
        wrapper.and(AI_USAGE_RECORD.PROVIDER.eq(request.getProvider(), StringUtils.isNotBlank(request.getProvider())));
        wrapper.and(AI_USAGE_RECORD.MODEL.eq(request.getModel(), StringUtils.isNotBlank(request.getModel())));
        wrapper.and(AI_USAGE_RECORD.PHASE.eq(request.getPhase(), StringUtils.isNotBlank(request.getPhase())));
        wrapper.and(AI_USAGE_RECORD.REQUEST_TYPE.eq(request.getRequestType(),
                StringUtils.isNotBlank(request.getRequestType())));
        wrapper.and(AI_USAGE_RECORD.USED_AT.ge(request.getStartTime(), request.getStartTime() != null));
        wrapper.and(AI_USAGE_RECORD.USED_AT.le(request.getEndTime(), request.getEndTime() != null));
        wrapper.orderBy(AI_USAGE_RECORD.USED_AT, false);
        wrapper.orderBy(AI_USAGE_RECORD.ID, false);
        return wrapper;
    }

    @Override
    public AiUsageSummaryVO getSummary(AiUsageQueryRequest request) {
        List<AiUsageRecord> records = this.list(getQueryWrapper(request == null ? new AiUsageQueryRequest() : request));
        AiUsageSummaryVO summary = new AiUsageSummaryVO();
        summary.setCallCount((long) records.size());
        summary.setPromptTokens(records.stream().mapToLong(record -> safeToken(record.getPromptTokens())).sum());
        summary.setCompletionTokens(records.stream().mapToLong(record -> safeToken(record.getCompletionTokens())).sum());
        summary.setTotalTokens(records.stream().mapToLong(record -> safeToken(record.getTotalTokens())).sum());
        summary.setCreditCost(amountNormalizer.normalize(records.stream()
                .map(AiUsageRecord::getCreditCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        summary.setModelItems(groupByLabel(records, record -> record.getProvider() + "/" + record.getModel()));
        summary.setPhaseItems(groupByLabel(records, AiUsageRecord::getPhase));
        return summary;
    }

    @Override
    public Page<AiUsageUserSummaryVO> listUserSummaryByPage(AiUsageQueryRequest request) {
        AiUsageQueryRequest query = request == null ? new AiUsageQueryRequest() : request;
        List<AiUsageUserSummaryVO> grouped = this.list(getQueryWrapper(query)).stream()
                .collect(Collectors.groupingBy(AiUsageRecord::getUserId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> buildUserSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AiUsageUserSummaryVO::getCreditCost,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int page = Math.max(1, query.getPage());
        int pageSize = Math.max(1, query.getPageSize());
        int fromIndex = Math.min((page - 1) * pageSize, grouped.size());
        int toIndex = Math.min(fromIndex + pageSize, grouped.size());
        Page<AiUsageUserSummaryVO> result = new Page<>(page, pageSize, grouped.size());
        result.setRecords(grouped.subList(fromIndex, toIndex));
        return result;
    }

    @Override
    public AiUsageRecordVO getUsageRecordVO(AiUsageRecord record) {
        if (record == null) {
            return null;
        }
        AiUsageRecordVO vo = new AiUsageRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setTaskId(record.getTaskId());
        vo.setAgentName(record.getAgentName());
        vo.setPhase(record.getPhase());
        vo.setProvider(record.getProvider());
        vo.setModel(record.getModel());
        vo.setRequestType(record.getRequestType());
        vo.setPromptTokens(record.getPromptTokens());
        vo.setCompletionTokens(record.getCompletionTokens());
        vo.setTotalTokens(record.getTotalTokens());
        vo.setCreditCost(record.getCreditCost());
        vo.setLatencyMs(record.getLatencyMs());
        vo.setResponseOk(record.getResponseOk());
        vo.setErrorMessage(record.getErrorMessage());
        vo.setUsedAt(record.getUsedAt());
        return vo;
    }

    @Override
    public List<AiUsageRecordVO> getUsageRecordVO(List<AiUsageRecord> records) {
        if (CollUtil.isEmpty(records)) {
            return new ArrayList<>();
        }
        return records.stream().map(this::getUsageRecordVO).toList();
    }

    private List<AiUsageSummaryItemVO> groupByLabel(List<AiUsageRecord> records,
            java.util.function.Function<AiUsageRecord, String> labelGetter) {
        return records.stream()
                .collect(Collectors.groupingBy(record -> StringUtils.defaultIfBlank(labelGetter.apply(record), "-"),
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> buildSummaryItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AiUsageSummaryItemVO::getCreditCost,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private AiUsageSummaryItemVO buildSummaryItem(String label, List<AiUsageRecord> records) {
        AiUsageSummaryItemVO item = new AiUsageSummaryItemVO();
        item.setLabel(label);
        item.setCallCount((long) records.size());
        item.setTotalTokens(records.stream().mapToLong(record -> safeToken(record.getTotalTokens())).sum());
        item.setCreditCost(amountNormalizer.normalize(records.stream()
                .map(AiUsageRecord::getCreditCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return item;
    }

    private AiUsageUserSummaryVO buildUserSummary(Long userId, List<AiUsageRecord> records) {
        AiUsageUserSummaryVO vo = new AiUsageUserSummaryVO();
        vo.setUserId(userId);
        vo.setCallCount((long) records.size());
        vo.setTotalTokens(records.stream().mapToLong(record -> safeToken(record.getTotalTokens())).sum());
        vo.setCreditCost(amountNormalizer.normalize(records.stream()
                .map(AiUsageRecord::getCreditCost)
                .filter(cost -> cost != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        return vo;
    }

    private Long resolveTotalTokens(RecordAiUsageCommand command) {
        if (command.getTotalTokens() != null) {
            return safeToken(command.getTotalTokens());
        }
        return safeToken(command.getPromptTokens()) + safeToken(command.getCompletionTokens());
    }

    private long safeToken(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }
}
