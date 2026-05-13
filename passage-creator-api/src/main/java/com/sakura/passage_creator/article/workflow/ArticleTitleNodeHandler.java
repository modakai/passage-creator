package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.article.agent.TitleGeneratorAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 标题生成节点处理器。
 */
@Component
public class ArticleTitleNodeHandler implements NodeAction {

    private final TitleGeneratorAgent titleGeneratorAgent;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;

    public ArticleTitleNodeHandler(TitleGeneratorAgent titleGeneratorAgent,
            ArticleWorkflowAdapter articleWorkflowAdapter) {
        this.titleGeneratorAgent = titleGeneratorAgent;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        // Graph 只传递通用状态，文章节点先还原为业务可读的 WorkflowContext。
        WorkflowContext context = WorkflowContext.fromMap(stateSnapshot.data());
        String taskId = context.getString("taskId");
        articleWorkflowAdapter.markPhase(taskId, ArticleStatusEnum.PROCESSING, ArticlePhaseEnum.TITLE_GENERATING);
        ArticleState state = articleWorkflowAdapter.toArticleState(context);
        titleGeneratorAgent.generatorTitle(state, state.getUserId());
        articleWorkflowAdapter.saveTitleOptions(taskId, state.getTitleOptions());
        // 返回值会进入 OverAllState，并被 interrupt 后的人工任务作为输入快照展示。
        return Map.of("titleOptions", state.getTitleOptions());
    }
}
