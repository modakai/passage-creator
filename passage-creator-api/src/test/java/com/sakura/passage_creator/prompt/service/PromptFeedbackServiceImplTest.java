package com.sakura.passage_creator.prompt.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.prompt.api.PromptUsageLogService;
import com.sakura.passage_creator.prompt.controller.PromptFeedbackController;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptFeedbackSubmitRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptFeedback;
import com.sakura.passage_creator.prompt.model.entity.PromptUsageLog;
import com.sakura.passage_creator.prompt.model.enums.PromptFeedbackRatingEnum;
import com.sakura.passage_creator.prompt.model.enums.PromptFeedbackStageEnum;
import com.sakura.passage_creator.prompt.model.vo.PromptFeedbackStatsVO;
import com.sakura.passage_creator.prompt.service.impl.PromptFeedbackServiceImpl;
import com.sakura.passage_creator.shared.annotation.AuthCheck;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prompt 反馈服务测试，覆盖反馈去重、任务归属和统计口径。
 */
class PromptFeedbackServiceImplTest {

    @Test
    void shouldUpsertFeedbackWithPromptUsageSnapshot() {
        ArticleService articleService = mock(ArticleService.class);
        PromptUsageLogService usageLogService = mock(PromptUsageLogService.class);
        TestablePromptFeedbackService service = new TestablePromptFeedbackService(articleService, usageLogService);
        LoginUserInfo user = user(1001L);
        when(articleService.getOwnedArticleByTaskId("task-1", user)).thenReturn(article("task-1", 1001L));
        service.matchedUsageLog = usageLog(10L, 20L, "article.title.user", "1.0.0");

        PromptFeedbackSubmitRequest firstRequest = submitRequest("task-1",
                PromptFeedbackStageEnum.TITLE_SELECTION.getValue(),
                PromptFeedbackRatingEnum.SATISFIED.getValue(),
                null);
        PromptFeedbackSubmitRequest secondRequest = submitRequest("task-1",
                PromptFeedbackStageEnum.TITLE_SELECTION.getValue(),
                PromptFeedbackRatingEnum.UNSATISFIED.getValue(),
                null);

        service.submitFeedback(firstRequest, user);
        service.submitFeedback(secondRequest, user);

        assertThat(service.feedbackStore).hasSize(1);
        PromptFeedback saved = service.feedbackStore.get(0);
        assertThat(saved.getRating()).isEqualTo(PromptFeedbackRatingEnum.UNSATISFIED.getValue());
        assertThat(saved.getRemark()).isNull();
        assertThat(saved.getPromptUsageLogId()).isEqualTo(10L);
        assertThat(saved.getPromptTemplateId()).isEqualTo(20L);
        assertThat(saved.getTemplateKey()).isEqualTo("article.title.user");
        assertThat(saved.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldRejectFeedbackWhenTaskDoesNotBelongToUser() {
        ArticleService articleService = mock(ArticleService.class);
        PromptUsageLogService usageLogService = mock(PromptUsageLogService.class);
        TestablePromptFeedbackService service = new TestablePromptFeedbackService(articleService, usageLogService);
        LoginUserInfo user = user(1001L);
        when(articleService.getOwnedArticleByTaskId("task-2", user))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND_ERROR));

        PromptFeedbackSubmitRequest request = submitRequest("task-2",
                PromptFeedbackStageEnum.OUTLINE_EDITING.getValue(),
                PromptFeedbackRatingEnum.SATISFIED.getValue(),
                null);

        assertThatThrownBy(() -> service.submitFeedback(request, user))
                .isInstanceOf(BusinessException.class);
        assertThat(service.feedbackStore).isEmpty();
    }

    @Test
    void shouldCalculateFourRatingRatiosFromSubmittedFeedbackOnly() {
        TestablePromptFeedbackService service = new TestablePromptFeedbackService(
                mock(ArticleService.class), mock(PromptUsageLogService.class));
        service.feedbackStore.add(feedback(PromptFeedbackStageEnum.TITLE_SELECTION.getValue(),
                PromptFeedbackRatingEnum.VERY_SATISFIED.getValue(), "article.title.user", "1.0.0"));
        service.feedbackStore.add(feedback(PromptFeedbackStageEnum.TITLE_SELECTION.getValue(),
                PromptFeedbackRatingEnum.SATISFIED.getValue(), "article.title.user", "1.0.0"));
        service.feedbackStore.add(feedback(PromptFeedbackStageEnum.TITLE_SELECTION.getValue(),
                PromptFeedbackRatingEnum.NEUTRAL.getValue(), "article.title.user", "1.0.0"));
        service.feedbackStore.add(feedback(PromptFeedbackStageEnum.TITLE_SELECTION.getValue(),
                PromptFeedbackRatingEnum.UNSATISFIED.getValue(), "article.title.user", "1.0.0"));
        service.feedbackStore.add(feedback(PromptFeedbackStageEnum.OUTLINE_EDITING.getValue(),
                PromptFeedbackRatingEnum.UNSATISFIED.getValue(), "article.outline.user", "1.0.0"));
        PromptFeedbackQueryRequest request = new PromptFeedbackQueryRequest();
        request.setTemplateKey("article.title.user");
        request.setVersion("1.0.0");

        List<PromptFeedbackStatsVO> stats = service.listStats(request);

        PromptFeedbackStatsVO titleStats = stats.stream()
                .filter(item -> PromptFeedbackStageEnum.TITLE_SELECTION.getValue().equals(item.getFeedbackStage()))
                .findFirst()
                .orElseThrow();
        assertThat(titleStats.getVerySatisfiedCount()).isEqualTo(1);
        assertThat(titleStats.getSatisfiedCount()).isEqualTo(1);
        assertThat(titleStats.getNeutralCount()).isEqualTo(1);
        assertThat(titleStats.getUnsatisfiedCount()).isEqualTo(1);
        assertThat(titleStats.getTotalCount()).isEqualTo(4);
        assertThat(titleStats.getVerySatisfiedRatio()).isEqualByComparingTo("0.2500");
        assertThat(titleStats.getSatisfiedRatio()).isEqualByComparingTo("0.2500");
        assertThat(titleStats.getNeutralRatio()).isEqualByComparingTo("0.2500");
        assertThat(titleStats.getUnsatisfiedRatio()).isEqualByComparingTo("0.2500");
        assertThat(stats.stream()
                .filter(item -> PromptFeedbackStageEnum.OUTLINE_EDITING.getValue().equals(item.getFeedbackStage()))
                .findFirst()
                .orElseThrow()
                .getTotalCount()).isZero();
    }

    @Test
    void shouldRequireAdminRoleForManagementEndpoints() throws NoSuchMethodException {
        AuthCheck listAuth = PromptFeedbackController.class
                .getMethod("listFeedbackByPage", PromptFeedbackQueryRequest.class)
                .getAnnotation(AuthCheck.class);
        AuthCheck statsAuth = PromptFeedbackController.class
                .getMethod("listStats", PromptFeedbackQueryRequest.class)
                .getAnnotation(AuthCheck.class);

        assertThat(listAuth.mustRole()).isEqualTo(UserConstant.ADMIN_ROLE);
        assertThat(statsAuth.mustRole()).isEqualTo(UserConstant.ADMIN_ROLE);
    }

    /**
     * 构造登录用户快照。
     */
    private LoginUserInfo user(Long userId) {
        return new LoginUserInfo(userId, "user" + userId, "用户" + userId, "user");
    }

    /**
     * 构造文章任务快照。
     */
    private Article article(String taskId, Long userId) {
        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(userId);
        return article;
    }

    /**
     * 构造反馈提交请求。
     */
    private PromptFeedbackSubmitRequest submitRequest(String taskId, String stage, String rating, String remark) {
        PromptFeedbackSubmitRequest request = new PromptFeedbackSubmitRequest();
        request.setTaskId(taskId);
        request.setFeedbackStage(stage);
        request.setRating(rating);
        request.setRemark(remark);
        return request;
    }

    /**
     * 构造 Prompt 使用日志快照。
     */
    private PromptUsageLog usageLog(Long id, Long templateId, String templateKey, String version) {
        PromptUsageLog usageLog = new PromptUsageLog();
        usageLog.setId(id);
        usageLog.setPromptTemplateId(templateId);
        usageLog.setTemplateKey(templateKey);
        usageLog.setVersion(version);
        usageLog.setEnvironment("default");
        usageLog.setUsedAt(LocalDateTime.now());
        return usageLog;
    }

    /**
     * 构造已落库反馈记录。
     */
    private PromptFeedback feedback(String stage, String rating, String templateKey, String version) {
        PromptFeedback feedback = new PromptFeedback();
        feedback.setFeedbackStage(stage);
        feedback.setRating(rating);
        feedback.setTemplateKey(templateKey);
        feedback.setVersion(version);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        return feedback;
    }

    /**
     * 测试替身只替换持久化边界，保留服务真实业务规则。
     */
    private static class TestablePromptFeedbackService extends PromptFeedbackServiceImpl {

        private final List<PromptFeedback> feedbackStore = new ArrayList<>();

        private PromptUsageLog matchedUsageLog;

        TestablePromptFeedbackService(ArticleService articleService, PromptUsageLogService promptUsageLogService) {
            super(articleService, promptUsageLogService);
        }

        @Override
        protected PromptFeedback findFeedback(Long userId, String taskId, String feedbackStage) {
            return feedbackStore.stream()
                    .filter(item -> userId.equals(item.getUserId()))
                    .filter(item -> taskId.equals(item.getTaskId()))
                    .filter(item -> feedbackStage.equals(item.getFeedbackStage()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        protected PromptUsageLog findMatchedUsageLog(String taskId, String feedbackStage) {
            return matchedUsageLog;
        }

        @Override
        protected boolean saveFeedback(PromptFeedback feedback) {
            feedbackStore.add(feedback);
            return true;
        }

        @Override
        protected boolean updateFeedback(PromptFeedback feedback) {
            return true;
        }

        @Override
        protected List<PromptFeedback> listFeedback(QueryWrapper queryWrapper) {
            return feedbackStore;
        }
    }
}
