package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.sakura.passage_creator.rednote.agent.RednoteContentAgent;
import com.sakura.passage_creator.rednote.agent.RednoteSearchAgent;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * 小红书创作 StateGraph 工厂，直接使用 Spring AI Alibaba Workflow 编排文章节点。
 */
@Component
@RequiredArgsConstructor
public class RednoteWorkflowGraphFactory {

    private final RednoteSearchAgent rednoteSearchAgent;

    private final RednoteContentAgent rednoteContentAgent;

    public StateGraph buildGraph() throws GraphStateException {

        // 构建包含 Agent 的工作流
        StateGraph workflow = new StateGraph(createKeyStrategyFactory());

        // 将搜索和文案 Agent 作为 SubGraph Node 添加，后续再串接图片提示词与图片生成节点。
        workflow.addNode(rednoteSearchAgent.name(), rednoteSearchAgent.asNode());
        workflow.addNode(rednoteContentAgent.name(), rednoteContentAgent.asNode());

        // 绘制边
        workflow.addEdge(START, rednoteSearchAgent.name());
        workflow.addEdge(rednoteSearchAgent.name(), rednoteContentAgent.name());
        workflow.addEdge(rednoteContentAgent.name(), END);

        return workflow;
    }

    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(RednoteWorkflowState.KEY_TASK_ID, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_USER_ID, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_CONTENT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.SEARCH_OUTPUT_KEY, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_SEARCH_RESPONSE, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_SUBJECT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_CONTEXT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_CONTENT_LENGTH, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_TARGET_WORD_COUNT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_KEYWORDS, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_TAG_COUNT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_IMAGE_COUNT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_SEARCH_RESULTS, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_COPYWRITING, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_BODY_CONTENT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_TAGS, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_COVER_TITLE, new ReplaceStrategy());
            return strategies;
        };
    }
}
