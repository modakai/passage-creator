package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.sakura.passage_creator.creation.workflow.checkpoint.RedisWorkflowCheckpointSaver;
import com.sakura.passage_creator.rednote.agent.RednoteCoverImagePromptAgent;
import com.sakura.passage_creator.rednote.agent.RednoteContentAgent;
import com.sakura.passage_creator.rednote.agent.RednoteNormalImagePromptAgent;
import com.sakura.passage_creator.rednote.agent.RednoteSearchAgent;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 小红书创作 StateGraph 工厂，直接使用 Spring AI Alibaba Workflow 编排文章节点。
 */
@Component
@RequiredArgsConstructor
public class RednoteWorkflowGraphFactory {

    private final RednoteSearchAgent rednoteSearchAgent;

    private final RednoteContentAgent rednoteContentAgent;

    private final RednoteNormalImagePromptAgent rednoteNormalImagePromptAgent;

    private final RednoteCoverImagePromptAgent rednoteCoverImagePromptAgent;

    private final RednoteImagePromptCompleteNodeHandler rednoteImagePromptCompleteNodeHandler;

    private final RednoteNormalImageGenerateNodeHandler rednoteNormalImageGenerateNodeHandler;

    private final RednoteCoverImageGenerateNodeHandler rednoteCoverImageGenerateNodeHandler;

    private final RednoteWorkflowCompleteNodeHandler rednoteWorkflowCompleteNodeHandler;

    private final RedisWorkflowCheckpointSaver checkpointSaver;

    /**
     * 编译 rednote workflow，并注册 Redis checkpoint，便于失败重试和后台状态排查。
     */
    public CompiledGraph compile() throws GraphStateException {
        return compile(null);
    }

    /**
     * 编译 rednote workflow，并允许调用方订阅节点生命周期事件。
     */
    public CompiledGraph compile(GraphLifecycleListener listener) throws GraphStateException {
        CompileConfig.Builder builder = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(checkpointSaver).build());
        if (listener != null) {
            // 生命周期监听只负责同步 workflow_task 与 SSE，不参与节点路由。
            builder.withLifecycleListener(listener);
        }
        return buildGraph().compile(builder.build());
    }

    /**
     * 判断当前 taskId 是否还有可恢复 checkpoint。
     */
    public boolean hasCheckpoint(String taskId) {
        BaseCheckpointSaver saver = checkpointSaver;
        return saver.get(RunnableConfig.builder().threadId(taskId).build()).isPresent();
    }

    /**
     * 清理指定任务的 checkpoint，避免全自动重新执行时读取旧 Agent messages。
     */
    public void clearCheckpoint(String taskId) {
        checkpointSaver.release(RunnableConfig.builder().threadId(taskId).build());
    }

    public StateGraph buildGraph() throws GraphStateException {

        // 构建包含 Agent 的工作流
        StateGraph workflow = new StateGraph(createKeyStrategyFactory());

        // 将搜索和文案 Agent 作为父图主链路节点添加。
        workflow.addNode(rednoteSearchAgent.name(), rednoteSearchAgent.asNode());
        workflow.addNode(rednoteContentAgent.name(), rednoteContentAgent.asNode());
        // 提示词 Agent 交给父图执行，确保 Agent Hook 的状态更新能被 Graph 合并。
        workflow.addNode(rednoteNormalImagePromptAgent.name(), rednoteNormalImagePromptAgent.asNode());
        workflow.addNode(rednoteCoverImagePromptAgent.name(), rednoteCoverImagePromptAgent.asNode());
        workflow.addNode(RednoteWorkflowNodeType.IMAGE_PROMPT_COMPLETED.getValue(), node_async(rednoteImagePromptCompleteNodeHandler));
        workflow.addNode(RednoteWorkflowNodeType.NORMAL_IMAGE_GENERATING.getValue(), node_async(rednoteNormalImageGenerateNodeHandler));
        workflow.addNode(RednoteWorkflowNodeType.COVER_IMAGE_GENERATING.getValue(), node_async(rednoteCoverImageGenerateNodeHandler));
        workflow.addNode(RednoteWorkflowNodeType.COMPLETED.getValue(), node_async(rednoteWorkflowCompleteNodeHandler));

        // 第一段并行：普通图片提示词和封面提示词并行生成，然后汇聚到提示词完成节点。
        workflow.addEdge(START, rednoteSearchAgent.name());
        workflow.addEdge(rednoteSearchAgent.name(), rednoteContentAgent.name());
        workflow.addEdge(rednoteContentAgent.name(), java.util.List.of(
                rednoteNormalImagePromptAgent.name(),
                rednoteCoverImagePromptAgent.name()
        ));
        workflow.addEdge(rednoteNormalImagePromptAgent.name(), RednoteWorkflowNodeType.IMAGE_PROMPT_COMPLETED.getValue());
        workflow.addEdge(rednoteCoverImagePromptAgent.name(), RednoteWorkflowNodeType.IMAGE_PROMPT_COMPLETED.getValue());

        // 第二段并行：提示词都准备好后，普通配图和封面图并行生成，最后汇聚到完成节点。
        workflow.addEdge(RednoteWorkflowNodeType.IMAGE_PROMPT_COMPLETED.getValue(), java.util.List.of(
                RednoteWorkflowNodeType.NORMAL_IMAGE_GENERATING.getValue(),
                RednoteWorkflowNodeType.COVER_IMAGE_GENERATING.getValue()
        ));
        workflow.addEdge(RednoteWorkflowNodeType.NORMAL_IMAGE_GENERATING.getValue(), RednoteWorkflowNodeType.COMPLETED.getValue());
        workflow.addEdge(RednoteWorkflowNodeType.COVER_IMAGE_GENERATING.getValue(), RednoteWorkflowNodeType.COMPLETED.getValue());
        workflow.addEdge(RednoteWorkflowNodeType.COMPLETED.getValue(), END);

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
            strategies.put(RednoteWorkflowState.KEY_NORMAL_IMAGE_PROMPT_RESPONSE, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_COVER_IMAGE_PROMPT_RESPONSE, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_IMAGE_PROMPTS, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_COVER_PROMPT, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_IMAGES, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_COVER_IMAGE, new ReplaceStrategy());
            strategies.put(RednoteWorkflowState.KEY_COVER_IMAGE_RESULT, new ReplaceStrategy());
            return strategies;
        };
    }
}
