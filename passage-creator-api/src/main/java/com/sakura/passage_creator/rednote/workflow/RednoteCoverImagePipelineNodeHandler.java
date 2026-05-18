package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.sakura.passage_creator.rednote.agent.RednoteCoverImagePromptAgent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 封面图流水线节点，内部串行执行“封面提示词 Agent -> 封面图生成”。
 */
@Component
public class RednoteCoverImagePipelineNodeHandler implements NodeAction {

    private final RednoteCoverImagePromptAgent coverImagePromptAgent;

    private final RednoteCoverImageGenerateNodeHandler coverImageGenerateNodeHandler;

    public RednoteCoverImagePipelineNodeHandler(RednoteCoverImagePromptAgent coverImagePromptAgent,
            RednoteCoverImageGenerateNodeHandler coverImageGenerateNodeHandler) {
        this.coverImagePromptAgent = coverImagePromptAgent;
        this.coverImageGenerateNodeHandler = coverImageGenerateNodeHandler;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) throws Exception {
        Map<String, Object> updates = new LinkedHashMap<>();
        Map<String, Object> promptUpdates = runAgentNode(coverImagePromptAgent.asNode(), stateSnapshot);
        updates.putAll(promptUpdates);

        Map<String, Object> nextState = new LinkedHashMap<>(stateSnapshot.data());
        nextState.putAll(promptUpdates);
        Map<String, Object> imageUpdates = coverImageGenerateNodeHandler.apply(new OverAllState(nextState));
        updates.putAll(imageUpdates);
        return updates;
    }

    /**
     * 直接执行 Agent-as-Node 的 action，保留 Agent Hook 的持久化能力。
     */
    private Map<String, Object> runAgentNode(Node node, OverAllState stateSnapshot) throws Exception {
        return node.actionFactory()
                .apply(CompileConfig.builder().build())
                .apply(stateSnapshot, RunnableConfig.builder().build())
                .get();
    }
}
