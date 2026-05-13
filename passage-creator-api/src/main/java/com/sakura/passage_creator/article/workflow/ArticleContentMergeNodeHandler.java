package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.image.service.ContentImageMerger;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 图文合成节点处理器。
 */
@Component
public class ArticleContentMergeNodeHandler implements NodeAction {

    private final ContentImageMerger contentImageMerger;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;

    public ArticleContentMergeNodeHandler(ContentImageMerger contentImageMerger,
            ArticleWorkflowAdapter articleWorkflowAdapter) {
        this.contentImageMerger = contentImageMerger;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        // 合成节点是最后一个业务节点，成功后会把文章正式结果写回 article 表。
        WorkflowContext context = WorkflowContext.fromMap(stateSnapshot.data());
        String taskId = context.getString("taskId");
        articleWorkflowAdapter.markPhase(taskId, ArticleStatusEnum.PROCESSING, ArticlePhaseEnum.CONTENT_MERGING);
        ArticleState state = articleWorkflowAdapter.toArticleState(context);
        List<ArticleState.ImageResult> images =
                cn.hutool.json.JSONUtil.toList(cn.hutool.json.JSONUtil.toJsonStr(context.getValues().get("images")),
                        ArticleState.ImageResult.class);
        state.setImages(images);
        state.setCoverImage(resolveCoverImage(images));
        String fullContent = contentImageMerger.merge(state.getContent(), images);
        state.setFullContent(fullContent);
        articleWorkflowAdapter.completeArticle(taskId, state);
        return Map.of("fullContent", fullContent);
    }

    private String resolveCoverImage(List<ArticleState.ImageResult> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(image -> image.getPosition() != null && image.getPosition() == 1)
                .map(ArticleState.ImageResult::getUrl)
                .findFirst()
                .orElse(null);
    }
}
