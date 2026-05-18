package com.sakura.passage_creator.prompt.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackSubmitRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptFeedback;
import com.sakura.passage_creator.prompt.model.entity.PromptUsageLog;
import com.sakura.passage_creator.prompt.model.enums.PromptFeedbackRatingEnum;
import com.sakura.passage_creator.prompt.model.enums.PromptFeedbackStageEnum;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackStatsVO;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackVO;
import com.sakura.passage_creator.prompt.repository.PromptFeedbackMapper;
import com.sakura.passage_creator.prompt.service.PromptFeedbackService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sakura.passage_creator.prompt.model.entity.table.PromptFeedbackTableDef.PROMPT_FEEDBACK;
import static com.sakura.passage_creator.prompt.model.entity.table.PromptUsageLogTableDef.PROMPT_USAGE_LOG;

/**
 * Prompt 反馈服务实现。
 */
@Service
public class PromptFeedbackServiceImpl extends ServiceImpl<PromptFeedbackMapper, PromptFeedback>
        implements PromptFeedbackService {

    /**
     * 标题反馈优先关联用户 Prompt 日志。
     */
    private static final String TITLE_USER_TEMPLATE_KEY = "article.title.user";

    /**
     * 大纲反馈优先关联用户 Prompt 日志。
     */
    private static final String OUTLINE_USER_TEMPLATE_KEY = "article.outline.user";

    /**
     * 正文融合反馈优先关联正文用户 Prompt 日志。
     */
    private static final String CONTENT_USER_TEMPLATE_KEY = "article.content.user";

    /**
     * 备注最大长度，防止管理端展示和数据库写入被大文本拖垮。
     */
    private static final int MAX_REMARK_LENGTH = 1000;

    /**
     * Prompt 使用日志服务，用于关联模板版本快照。
     */
    private final PromptUsageLogService promptUsageLogService;

    public PromptFeedbackServiceImpl(PromptUsageLogService promptUsageLogService) {
        this.promptUsageLogService = promptUsageLogService;
    }

    @Override
    public PromptFeedbackVO submitFeedback(PromptFeedbackSubmitRequest request, LoginUserInfo loginUser) {
        validateSubmitRequest(request, loginUser);
        PromptUsageLog usageLog = findMatchedUsageLog(request.getTaskId(), request.getFeedbackStage());
        assertUsageLogBelongsToUser(usageLog, loginUser);

        LocalDateTime now = LocalDateTime.now();
        PromptFeedback existing = findFeedback(loginUser.userId(), request.getTaskId(), request.getFeedbackStage());
        PromptFeedback feedback = existing == null ? new PromptFeedback() : existing;
        feedback.setUserId(loginUser.userId());
        feedback.setTaskId(request.getTaskId());
        feedback.setFeedbackStage(request.getFeedbackStage());
        feedback.setRating(request.getRating());
        feedback.setRemark(StringUtils.trimToNull(request.getRemark()));
        feedback.setUpdateTime(now);
        if (feedback.getCreateTime() == null) {
            feedback.setCreateTime(now);
        }
        if (feedback.getIsDelete() == null) {
            feedback.setIsDelete(0);
        }
        fillPromptSnapshot(feedback, usageLog);

        boolean result = existing == null ? saveFeedback(feedback) : updateFeedback(feedback);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "反馈保存失败");
        return getFeedbackVO(feedback);
    }

    @Override
    public QueryWrapper getQueryWrapper(PromptFeedbackQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(PROMPT_FEEDBACK.USER_ID.eq(request.getUserId(), request.getUserId() != null));
        wrapper.and(PROMPT_FEEDBACK.TASK_ID.eq(request.getTaskId(), StringUtils.isNotBlank(request.getTaskId())));
        wrapper.and(PROMPT_FEEDBACK.FEEDBACK_STAGE.eq(request.getFeedbackStage(),
                StringUtils.isNotBlank(request.getFeedbackStage())));
        wrapper.and(PROMPT_FEEDBACK.RATING.eq(request.getRating(), StringUtils.isNotBlank(request.getRating())));
        wrapper.and(PROMPT_FEEDBACK.TEMPLATE_KEY.eq(request.getTemplateKey(),
                StringUtils.isNotBlank(request.getTemplateKey())));
        wrapper.and(PROMPT_FEEDBACK.VERSION.eq(request.getVersion(), StringUtils.isNotBlank(request.getVersion())));
        wrapper.and(PROMPT_FEEDBACK.CREATE_TIME.ge(request.getStartTime(), request.getStartTime() != null));
        wrapper.and(PROMPT_FEEDBACK.CREATE_TIME.le(request.getEndTime(), request.getEndTime() != null));
        wrapper.orderBy(PROMPT_FEEDBACK.CREATE_TIME, false);
        wrapper.orderBy(PROMPT_FEEDBACK.ID, false);
        return wrapper;
    }

    @Override
    public List<PromptFeedbackStatsVO> listStats(PromptFeedbackQueryRequest request) {
        PromptFeedbackQueryRequest safeRequest = request == null ? new PromptFeedbackQueryRequest() : request;
        List<PromptFeedback> records = listFeedback(getQueryWrapper(safeRequest)).stream()
                .filter(record -> matchesStatsFilter(record, safeRequest))
                .toList();
        Map<String, List<PromptFeedback>> recordsByStage = new LinkedHashMap<>();
        for (PromptFeedbackStageEnum stage : PromptFeedbackStageEnum.orderedStages()) {
            recordsByStage.put(stage.getValue(), new ArrayList<>());
        }
        for (PromptFeedback record : records) {
            recordsByStage.computeIfAbsent(record.getFeedbackStage(), ignored -> new ArrayList<>()).add(record);
        }
        return recordsByStage.entrySet().stream()
                .map(entry -> buildStats(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public PromptFeedbackVO getFeedbackVO(PromptFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        PromptFeedbackVO vo = new PromptFeedbackVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setTaskId(feedback.getTaskId());
        vo.setFeedbackStage(feedback.getFeedbackStage());
        vo.setFeedbackStageLabel(resolveStageLabel(feedback.getFeedbackStage()));
        vo.setRating(feedback.getRating());
        vo.setRatingLabel(resolveRatingLabel(feedback.getRating()));
        vo.setRemark(feedback.getRemark());
        vo.setPromptUsageLogId(feedback.getPromptUsageLogId());
        vo.setPromptTemplateId(feedback.getPromptTemplateId());
        vo.setTemplateKey(feedback.getTemplateKey());
        vo.setVersion(feedback.getVersion());
        vo.setEnvironment(feedback.getEnvironment());
        vo.setPromptLinked(feedback.getPromptUsageLogId() != null);
        vo.setCreateTime(feedback.getCreateTime());
        vo.setUpdateTime(feedback.getUpdateTime());
        return vo;
    }

    @Override
    public List<PromptFeedbackVO> getFeedbackVO(List<PromptFeedback> feedbackList) {
        if (CollUtil.isEmpty(feedbackList)) {
            return new ArrayList<>();
        }
        return feedbackList.stream().map(this::getFeedbackVO).toList();
    }

    /**
     * 查询同一用户、任务、环节的已有反馈，独立方法便于单元测试替换持久化边界。
     */
    protected PromptFeedback findFeedback(Long userId, String taskId, String feedbackStage) {
        return getOne(QueryWrapper.create()
                .where(PROMPT_FEEDBACK.USER_ID.eq(userId))
                .and(PROMPT_FEEDBACK.TASK_ID.eq(taskId))
                .and(PROMPT_FEEDBACK.FEEDBACK_STAGE.eq(feedbackStage)));
    }

    /**
     * 按任务和环节匹配最近的 Prompt 使用日志。
     */
    protected PromptUsageLog findMatchedUsageLog(String taskId, String feedbackStage) {
        Set<String> templateKeys = templateKeysForStage(feedbackStage);
        if (templateKeys.isEmpty()) {
            return null;
        }
        return promptUsageLogService.getOne(QueryWrapper.create()
                .where(PROMPT_USAGE_LOG.TASK_ID.eq(taskId))
                .and(PROMPT_USAGE_LOG.TEMPLATE_KEY.in(templateKeys))
                .orderBy(PROMPT_USAGE_LOG.USED_AT, false)
                .orderBy(PROMPT_USAGE_LOG.ID, false)
                .limit(1));
    }

    /**
     * 保存反馈记录，独立方法便于测试替换持久化边界。
     */
    protected boolean saveFeedback(PromptFeedback feedback) {
        return save(feedback);
    }

    /**
     * 更新反馈记录，独立方法便于测试替换持久化边界。
     */
    protected boolean updateFeedback(PromptFeedback feedback) {
        return updateById(feedback);
    }

    /**
     * 查询反馈记录列表，独立方法便于测试替换持久化边界。
     */
    protected List<PromptFeedback> listFeedback(QueryWrapper queryWrapper) {
        return list(queryWrapper);
    }

    /**
     * 校验反馈提交请求的业务边界。
     */
    private void validateSubmitRequest(PromptFeedbackSubmitRequest request, LoginUserInfo loginUser) {
        ThrowUtils.throwIf(loginUser == null || loginUser.userId() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null || StringUtils.isBlank(request.getTaskId()), ErrorCode.PARAMS_ERROR,
                "任务 id 不能为空");
        ThrowUtils.throwIf(!PromptFeedbackStageEnum.isValid(request.getFeedbackStage()), ErrorCode.PARAMS_ERROR,
                "反馈环节非法");
        ThrowUtils.throwIf(!PromptFeedbackRatingEnum.isValid(request.getRating()), ErrorCode.PARAMS_ERROR,
                "反馈满意度非法");
        ThrowUtils.throwIf(StringUtils.length(request.getRemark()) > MAX_REMARK_LENGTH, ErrorCode.PARAMS_ERROR,
                "反馈说明不能超过 1000 个字符");
    }

    /**
     * 用户端反馈以 Prompt 使用日志为归属凭证，避免 Prompt 模块反向依赖文章模块。
     */
    private void assertUsageLogBelongsToUser(PromptUsageLog usageLog, LoginUserInfo loginUser) {
        ThrowUtils.throwIf(usageLog == null || usageLog.getUserId() == null, ErrorCode.NOT_FOUND_ERROR,
                "未找到可反馈的 Prompt 使用记录");
        ThrowUtils.throwIf(!loginUser.userId().equals(usageLog.getUserId()), ErrorCode.NO_AUTH_ERROR,
                "不能提交他人任务的 Prompt 反馈");
    }

    /**
     * 将 Prompt 使用日志中的模板版本信息复制到反馈快照。
     */
    private void fillPromptSnapshot(PromptFeedback feedback, PromptUsageLog usageLog) {
        if (usageLog == null) {
            feedback.setPromptUsageLogId(null);
            feedback.setPromptTemplateId(null);
            feedback.setTemplateKey(null);
            feedback.setVersion(null);
            feedback.setEnvironment(null);
            return;
        }
        feedback.setPromptUsageLogId(usageLog.getId());
        feedback.setPromptTemplateId(usageLog.getPromptTemplateId());
        feedback.setTemplateKey(usageLog.getTemplateKey());
        feedback.setVersion(usageLog.getVersion());
        feedback.setEnvironment(usageLog.getEnvironment());
    }

    /**
     * 按反馈环节解析优先关联的模板标识集合。
     */
    private Set<String> templateKeysForStage(String feedbackStage) {
        PromptFeedbackStageEnum stage = PromptFeedbackStageEnum.of(feedbackStage);
        if (stage == null) {
            return Set.of();
        }
        return switch (stage) {
            case TITLE_SELECTION -> Set.of(TITLE_USER_TEMPLATE_KEY, "article.title.system");
            case OUTLINE_EDITING -> Set.of(OUTLINE_USER_TEMPLATE_KEY, "article.outline.system");
            case CONTENT_MERGED -> Set.of(CONTENT_USER_TEMPLATE_KEY, "article.content.system");
        };
    }

    /**
     * 构建单个环节的满意度占比统计。
     */
    private PromptFeedbackStatsVO buildStats(String feedbackStage, List<PromptFeedback> records) {
        long verySatisfiedCount = records.stream()
                .filter(item -> PromptFeedbackRatingEnum.VERY_SATISFIED.getValue().equals(item.getRating()))
                .count();
        long satisfiedCount = records.stream()
                .filter(item -> PromptFeedbackRatingEnum.SATISFIED.getValue().equals(item.getRating()))
                .count();
        long neutralCount = records.stream()
                .filter(item -> PromptFeedbackRatingEnum.NEUTRAL.getValue().equals(item.getRating()))
                .count();
        long unsatisfiedCount = records.stream()
                .filter(item -> PromptFeedbackRatingEnum.UNSATISFIED.getValue().equals(item.getRating()))
                .count();
        long totalCount = verySatisfiedCount + satisfiedCount + neutralCount + unsatisfiedCount;
        PromptFeedbackStatsVO vo = new PromptFeedbackStatsVO();
        vo.setFeedbackStage(feedbackStage);
        vo.setFeedbackStageLabel(resolveStageLabel(feedbackStage));
        vo.setVerySatisfiedCount(verySatisfiedCount);
        vo.setSatisfiedCount(satisfiedCount);
        vo.setNeutralCount(neutralCount);
        vo.setUnsatisfiedCount(unsatisfiedCount);
        vo.setTotalCount(totalCount);
        vo.setVerySatisfiedRatio(ratio(verySatisfiedCount, totalCount));
        vo.setSatisfiedRatio(ratio(satisfiedCount, totalCount));
        vo.setNeutralRatio(ratio(neutralCount, totalCount));
        vo.setUnsatisfiedRatio(ratio(unsatisfiedCount, totalCount));
        return vo;
    }

    /**
     * 统计层显式保留筛选口径，避免未来查询实现变化导致占比分母扩大。
     */
    private boolean matchesStatsFilter(PromptFeedback feedback, PromptFeedbackQueryRequest request) {
        if (feedback == null) {
            return false;
        }
        if (request.getUserId() != null && !request.getUserId().equals(feedback.getUserId())) {
            return false;
        }
        if (StringUtils.isNotBlank(request.getTaskId()) && !StringUtils.equals(request.getTaskId(), feedback.getTaskId())) {
            return false;
        }
        if (StringUtils.isNotBlank(request.getFeedbackStage())
                && !StringUtils.equals(request.getFeedbackStage(), feedback.getFeedbackStage())) {
            return false;
        }
        if (StringUtils.isNotBlank(request.getRating()) && !StringUtils.equals(request.getRating(), feedback.getRating())) {
            return false;
        }
        if (StringUtils.isNotBlank(request.getTemplateKey())
                && !StringUtils.equals(request.getTemplateKey(), feedback.getTemplateKey())) {
            return false;
        }
        if (StringUtils.isNotBlank(request.getVersion()) && !StringUtils.equals(request.getVersion(), feedback.getVersion())) {
            return false;
        }
        if (request.getStartTime() != null && (feedback.getCreateTime() == null
                || feedback.getCreateTime().isBefore(request.getStartTime()))) {
            return false;
        }
        return request.getEndTime() == null || (feedback.getCreateTime() != null
                && !feedback.getCreateTime().isAfter(request.getEndTime()));
    }

    /**
     * 计算四位小数占比。
     */
    private BigDecimal ratio(long count, long totalCount) {
        if (totalCount <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(count).divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP);
    }

    /**
     * 解析反馈环节展示名。
     */
    private String resolveStageLabel(String feedbackStage) {
        PromptFeedbackStageEnum stage = PromptFeedbackStageEnum.of(feedbackStage);
        return stage == null ? feedbackStage : stage.getLabel();
    }

    /**
     * 解析评价结果展示名。
     */
    private String resolveRatingLabel(String rating) {
        PromptFeedbackRatingEnum ratingEnum = PromptFeedbackRatingEnum.of(rating);
        return ratingEnum == null ? rating : ratingEnum.getLabel();
    }
}
