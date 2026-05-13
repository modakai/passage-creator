package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.article.agent.ImageAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 配图分析节点处理器。
 */
@Component
public class ArticleImageAnalyzeNodeHandler implements NodeAction {

    private final ImageAgent imageAgent;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;

    public ArticleImageAnalyzeNodeHandler(ImageAgent imageAgent, ArticleWorkflowAdapter articleWorkflowAdapter) {
        this.imageAgent = imageAgent;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        // 配图分析依赖正文内容，因此从 Graph 状态还原完整 ArticleState。
        WorkflowContext context = WorkflowContext.fromMap(stateSnapshot.data());
        String taskId = context.getString("taskId");
        articleWorkflowAdapter.markPhase(taskId, ArticleStatusEnum.PROCESSING, ArticlePhaseEnum.IMAGE_ANALYZING);
        ArticleState state = articleWorkflowAdapter.toArticleState(context);
        imageAgent.analyze(state);
        return Map.of(
                "content", state.getContent(),
                "imageRequirements", state.getImageRequirements()
        );
    }
}
