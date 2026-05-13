package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.article.agent.ImageAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 配图生成节点处理器。
 */
@Component
public class ArticleImageGenerateNodeHandler implements NodeAction {

    private final ImageAgent imageAgent;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;

    public ArticleImageGenerateNodeHandler(ImageAgent imageAgent, ArticleWorkflowAdapter articleWorkflowAdapter) {
        this.imageAgent = imageAgent;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        // imageRequirements 来自上一节点，需重新反序列化成强类型后交给图片工具。
        WorkflowContext context = WorkflowContext.fromMap(stateSnapshot.data());
        String taskId = context.getString("taskId");
        articleWorkflowAdapter.markPhase(taskId, ArticleStatusEnum.PROCESSING, ArticlePhaseEnum.IMAGE_GENERATING);
        ArticleState state = articleWorkflowAdapter.toArticleState(context);
        List<ArticleState.ImageRequirement> requirements =
                cn.hutool.json.JSONUtil.toList(cn.hutool.json.JSONUtil.toJsonStr(context.getValues().get("imageRequirements")),
                        ArticleState.ImageRequirement.class);
        state.setImageRequirements(requirements);
        List<ArticleState.ImageResult> images = imageAgent.generateImages(
                taskId,
                state.getUserId(),
                requirements,
                null
        );
        state.setImages(images);
        return Map.of("images", images);
    }
}
