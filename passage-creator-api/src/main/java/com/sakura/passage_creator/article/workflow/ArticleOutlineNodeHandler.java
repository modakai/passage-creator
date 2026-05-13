package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.article.agent.OutlineGeneratorAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 大纲生成节点处理器。
 */
@Component
public class ArticleOutlineNodeHandler implements NodeAction {

    private final OutlineGeneratorAgent outlineGeneratorAgent;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;

    public ArticleOutlineNodeHandler(OutlineGeneratorAgent outlineGeneratorAgent,
            ArticleWorkflowAdapter articleWorkflowAdapter) {
        this.outlineGeneratorAgent = outlineGeneratorAgent;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        // 标题确认结果已经在 resume 前写入 checkpoint，这里直接从 Graph 状态读取。
        WorkflowContext context = WorkflowContext.fromMap(stateSnapshot.data());
        String taskId = context.getString("taskId");
        articleWorkflowAdapter.saveSelectedTitle(taskId,
                context.getString("selectedMainTitle"),
                context.getString("selectedSubTitle"),
                context.getString("userDescription"));
        articleWorkflowAdapter.markPhase(taskId, ArticleStatusEnum.PROCESSING, ArticlePhaseEnum.OUTLINE_GENERATING);
        ArticleState state = articleWorkflowAdapter.toArticleState(context);
        outlineGeneratorAgent.generatorOutline(state, state.getUserId());
        articleWorkflowAdapter.saveOutlineDraft(taskId, state.getOutline());
        // 大纲草稿进入 Graph 状态后，会在 OUTLINE_GENERATING 的中断点展示给用户确认。
        return Map.of("outline", state.getOutline());
    }
}
