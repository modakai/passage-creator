package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.article.agent.ContentGeneratorAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.creation.workflow.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 正文生成节点处理器。
 */
@Component
public class ArticleContentNodeHandler implements NodeAction {

    private final ContentGeneratorAgent contentGeneratorAgent;
    private final ArticleWorkflowAdapter articleWorkflowAdapter;

    public ArticleContentNodeHandler(ContentGeneratorAgent contentGeneratorAgent,
            ArticleWorkflowAdapter articleWorkflowAdapter) {
        this.contentGeneratorAgent = contentGeneratorAgent;
        this.articleWorkflowAdapter = articleWorkflowAdapter;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        // confirmedOutline 是人工确认后的最终版本，正文生成不能再使用 AI 草稿大纲。
        WorkflowContext context = WorkflowContext.fromMap(stateSnapshot.data());
        String taskId = context.getString("taskId");
        ArticleState.OutlineResult confirmedOutline = context.getBean("confirmedOutline", ArticleState.OutlineResult.class);
        articleWorkflowAdapter.saveConfirmedOutline(taskId, confirmedOutline);
        articleWorkflowAdapter.markPhase(taskId, ArticleStatusEnum.PROCESSING, ArticlePhaseEnum.CONTENT_GENERATING);
        ArticleState state = articleWorkflowAdapter.toArticleState(context);
        contentGeneratorAgent.generatorContent(state, state.getUserId());
        // 正文暂存在 Graph 状态，后续配图分析节点会继续读取它。
        return Map.of("content", state.getContent());
    }
}
