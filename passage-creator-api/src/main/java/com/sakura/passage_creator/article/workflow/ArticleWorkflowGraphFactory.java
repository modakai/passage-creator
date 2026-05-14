package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.sakura.passage_creator.creation.workflow.checkpoint.RedisWorkflowCheckpointSaver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 文章创作 StateGraph 工厂，直接使用 Spring AI Alibaba Workflow 编排文章节点。
 */
@Component
public class ArticleWorkflowGraphFactory {

    private final NodeAction titleAction;
    private final NodeAction titleConfirmedAction;
    private final NodeAction outlineAction;
    private final NodeAction outlineConfirmedAction;
    private final NodeAction contentAction;
    private final NodeAction imageAnalyzeAction;
    private final NodeAction imageGenerateAction;
    private final NodeAction contentMergeAction;
    private final BaseCheckpointSaver checkpointSaver;

    @Autowired
    public ArticleWorkflowGraphFactory(ArticleTitleNodeHandler titleAction,
            ArticleTitleConfirmedNodeAction titleConfirmedAction,
            ArticleOutlineNodeHandler outlineAction,
            ArticleOutlineConfirmedNodeAction outlineConfirmedAction,
            ArticleContentNodeHandler contentAction,
            ArticleImageAnalyzeNodeHandler imageAnalyzeAction,
            ArticleImageGenerateNodeHandler imageGenerateAction,
            ArticleContentMergeNodeHandler contentMergeAction,
            RedisWorkflowCheckpointSaver checkpointSaver) {
        this((NodeAction) titleAction,
                titleConfirmedAction,
                outlineAction,
                outlineConfirmedAction,
                contentAction,
                imageAnalyzeAction,
                imageGenerateAction,
                contentMergeAction,
                checkpointSaver);
    }

    ArticleWorkflowGraphFactory(NodeAction titleAction,
            NodeAction titleConfirmedAction,
            NodeAction outlineAction,
            NodeAction outlineConfirmedAction,
            NodeAction contentAction,
            NodeAction imageAnalyzeAction,
            NodeAction imageGenerateAction,
            NodeAction contentMergeAction) {
        this(titleAction,
                titleConfirmedAction,
                outlineAction,
                outlineConfirmedAction,
                contentAction,
                imageAnalyzeAction,
                imageGenerateAction,
                contentMergeAction,
                MemorySaver.builder().build());
    }

    ArticleWorkflowGraphFactory(NodeAction titleAction,
            NodeAction titleConfirmedAction,
            NodeAction outlineAction,
            NodeAction outlineConfirmedAction,
            NodeAction contentAction,
            NodeAction imageAnalyzeAction,
            NodeAction imageGenerateAction,
            NodeAction contentMergeAction,
            BaseCheckpointSaver checkpointSaver) {
        this.titleAction = titleAction;
        this.titleConfirmedAction = titleConfirmedAction;
        this.outlineAction = outlineAction;
        this.outlineConfirmedAction = outlineConfirmedAction;
        this.contentAction = contentAction;
        this.imageAnalyzeAction = imageAnalyzeAction;
        this.imageGenerateAction = imageGenerateAction;
        this.contentMergeAction = contentMergeAction;
        // 生产环境注入 Redis saver；测试构造器仍使用 MemorySaver 保持单测轻量。
        this.checkpointSaver = checkpointSaver;
    }

    /**
     * 编译文章 workflow，并在需要人工确认的节点后设置框架级中断点。
     */
    public CompiledGraph compile() throws GraphStateException {
        return compile(null);
    }

    /**
     * 编译文章 workflow，并允许调用方订阅节点生命周期事件。
     */
    public CompiledGraph compile(GraphLifecycleListener listener) throws GraphStateException {
        CompileConfig.Builder builder = CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(checkpointSaver).build())
                // 标题和大纲生成后必须停住，把选择权交给用户，而不是自动流到下一节点。
                .interruptAfter(
                        ArticleWorkflowNodeType.TITLE_GENERATING.getValue(),
                        ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue()
                );
        if (listener != null) {
            // 生命周期监听只负责同步任务状态和 SSE，不参与 Graph 的下一节点决策。
            builder.withLifecycleListener(listener);
        }
        return buildGraph().compile(builder.build());
    }

    /**
     * 判断当前 taskId 是否还有可恢复 checkpoint，用于人工确认前拦截已过期流程。
     */
    public boolean hasCheckpoint(String taskId) {
        return checkpointSaver.get(com.alibaba.cloud.ai.graph.RunnableConfig.builder().threadId(taskId).build())
                .isPresent();
    }

    /**
     * 构建文章 workflow 的有向图：生成标题 -> 确认标题 -> 生成大纲 -> 确认大纲 -> 正文和配图。
     */
    public StateGraph buildGraph() throws GraphStateException {
        StateGraph graph = new StateGraph(createKeyStrategyFactory());
        // 所有业务节点直接注册为 Spring AI Alibaba NodeAction，避免再绕一层自定义 workflow 引擎。
        graph.addNode(ArticleWorkflowNodeType.TITLE_GENERATING.getValue(), node_async(titleAction));
        graph.addNode(ArticleWorkflowNodeType.TITLE_CONFIRM.getValue(), node_async(titleConfirmedAction));
        graph.addNode(ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue(), node_async(outlineAction));
        graph.addNode(ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue(), node_async(outlineConfirmedAction));
        graph.addNode(ArticleWorkflowNodeType.CONTENT_GENERATING.getValue(), node_async(contentAction));
        graph.addNode(ArticleWorkflowNodeType.IMAGE_ANALYZING.getValue(), node_async(imageAnalyzeAction));
        graph.addNode(ArticleWorkflowNodeType.IMAGE_GENERATING.getValue(), node_async(imageGenerateAction));
        graph.addNode(ArticleWorkflowNodeType.CONTENT_MERGING.getValue(), node_async(contentMergeAction));

        // 文章第一版是确定性链路，节点顺序用代码固定，后续 rednote 另建自己的 Graph。
        graph.addEdge(START, ArticleWorkflowNodeType.TITLE_GENERATING.getValue());
        graph.addEdge(ArticleWorkflowNodeType.TITLE_GENERATING.getValue(), ArticleWorkflowNodeType.TITLE_CONFIRM.getValue());
        graph.addEdge(ArticleWorkflowNodeType.TITLE_CONFIRM.getValue(), ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue());
        graph.addEdge(ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue(), ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue());
        graph.addEdge(ArticleWorkflowNodeType.OUTLINE_CONFIRM.getValue(), ArticleWorkflowNodeType.CONTENT_GENERATING.getValue());
        graph.addEdge(ArticleWorkflowNodeType.CONTENT_GENERATING.getValue(), ArticleWorkflowNodeType.IMAGE_ANALYZING.getValue());
        graph.addEdge(ArticleWorkflowNodeType.IMAGE_ANALYZING.getValue(), ArticleWorkflowNodeType.IMAGE_GENERATING.getValue());
        graph.addEdge(ArticleWorkflowNodeType.IMAGE_GENERATING.getValue(), ArticleWorkflowNodeType.CONTENT_MERGING.getValue());
        graph.addEdge(ArticleWorkflowNodeType.CONTENT_MERGING.getValue(), END);
        return graph;
    }

    private KeyStrategyFactory createKeyStrategyFactory() {
        return () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            for (String key : stateKeys()) {
                // 文章节点输出都是最新快照，后写入的人工确认结果应直接覆盖旧值。
                strategies.put(key, new ReplaceStrategy());
            }
            return strategies;
        };
    }

    private String[] stateKeys() {
        return new String[] {
                "taskId",
                "topic",
                "enabledImageMethods",
                "titleOptions",
                "selectedMainTitle",
                "selectedSubTitle",
                "userDescription",
                "outline",
                "confirmedOutline",
                "content",
                "imageRequirements",
                "images",
                "fullContent"
        };
    }
}
