package com.sakura.passage_creator.article.workflow;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sakura.passage_creator.article.model.entity.table.ArticleTableDef.ARTICLE;

/**
 * 文章 workflow 适配器，负责在通用上下文和 article 业务模型之间转换。
 */
@Component
public class ArticleWorkflowAdapter {

    private final ArticleService articleService;

    public ArticleWorkflowAdapter(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 从 workflow context 恢复文章状态。
     */
    public ArticleState toArticleState(WorkflowContext context) {
        String taskId = context.getString("taskId");
        Article article = getArticleByTaskId(taskId);
        ArticleState.TitleResult title = new ArticleState.TitleResult();
        title.setMainTitle(firstNotBlank(context.getString("selectedMainTitle"), article.getMainTitle()));
        title.setSubTitle(firstNotBlank(context.getString("selectedSubTitle"), article.getSubTitle()));
        ArticleState state = ArticleState.builder()
                .taskId(taskId)
                .userId(article.getUserId())
                .topic(article.getTopic())
                .userDescription(firstNotBlank(context.getString("userDescription"), article.getUserDescription()))
                .title(title)
                .outline(resolveOutline(context, article))
                .content(firstNotBlank(context.getString("content"), article.getContent()))
                .enabledImageMethods(parseEnabledImageMethods(article.getEnabledImageMethods()))
                .build();
        if (context.getValues().containsKey("imageRequirements")) {
            state.setImageRequirements(JSONUtil.toList(JSONUtil.toJsonStr(context.getValues().get("imageRequirements")),
                    ArticleState.ImageRequirement.class));
        }
        if (context.getValues().containsKey("images")) {
            state.setImages(JSONUtil.toList(JSONUtil.toJsonStr(context.getValues().get("images")),
                    ArticleState.ImageResult.class));
        }
        return state;
    }

    /**
     * 标记文章进入指定阶段。
     */
    public void markPhase(String taskId, ArticleStatusEnum status, ArticlePhaseEnum phase) {
        articleService.updateStatus(status, taskId);
        articleService.updatePhase(phase, taskId);
    }

    /**
     * 保存标题候选。
     */
    public void saveTitleOptions(String taskId, List<ArticleState.TitleOption> titleOptions) {
        articleService.saveTitleOptions(titleOptions, taskId);
        articleService.updateStatus(ArticleStatusEnum.PENDING, taskId);
        articleService.updatePhase(ArticlePhaseEnum.TITLE_SELECTING, taskId);
    }

    /**
     * 保存用户选择的标题。
     */
    public void saveSelectedTitle(String taskId, String mainTitle, String subTitle, String userDescription) {
        Article article = getArticleByTaskId(taskId);
        article.setMainTitle(mainTitle);
        article.setSubTitle(subTitle);
        article.setUserDescription(userDescription);
        article.setStatus(ArticleStatusEnum.PROCESSING.getValue());
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());
        articleService.updateById(article);
    }

    /**
     * 保存 AI 生成的大纲草稿。
     */
    public void saveOutlineDraft(String taskId, ArticleState.OutlineResult outline) {
        articleService.saveOutline(outline, taskId);
        articleService.updateStatus(ArticleStatusEnum.PENDING, taskId);
        articleService.updatePhase(ArticlePhaseEnum.OUTLINE_EDITING, taskId);
    }

    /**
     * 保存用户确认后的大纲。
     */
    public void saveConfirmedOutline(String taskId, ArticleState.OutlineResult outline) {
        articleService.saveOutline(outline, taskId);
        articleService.updateStatus(ArticleStatusEnum.PROCESSING, taskId);
        articleService.updatePhase(ArticlePhaseEnum.CONTENT_GENERATING, taskId);
    }

    /**
     * 保存最终图文结果。
     */
    public void completeArticle(String taskId, ArticleState state) {
        if (!articleService.completeContentWithImages(taskId, state)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存完整图文失败");
        }
    }

    /**
     * 获取文章实体。
     */
    public Article getArticleByTaskId(String taskId) {
        Article article = articleService.getOne(QueryWrapper.create().where(ARTICLE.TASK_ID.eq(taskId)));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文章任务不存在");
        }
        return article;
    }

    private ArticleState.OutlineResult resolveOutline(WorkflowContext context, Article article) {
        ArticleState.OutlineResult confirmed = context.getBean("confirmedOutline", ArticleState.OutlineResult.class);
        if (confirmed != null) {
            return confirmed;
        }
        ArticleState.OutlineResult generated = context.getBean("outline", ArticleState.OutlineResult.class);
        if (generated != null) {
            return generated;
        }
        if (article.getOutline() == null || article.getOutline().isBlank()) {
            return null;
        }
        return JSONUtil.toBean(article.getOutline(), ArticleState.OutlineResult.class);
    }

    private List<String> parseEnabledImageMethods(String enabledImageMethods) {
        if (enabledImageMethods == null || enabledImageMethods.isBlank()) {
            return List.of();
        }
        return JSONUtil.toList(enabledImageMethods, String.class);
    }

    private String firstNotBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
