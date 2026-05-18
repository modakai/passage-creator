package com.sakura.passage_creator.rednote.workflow;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.sakura.passage_creator.rednote.agent.RednoteNormalImagePromptAgent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 普通配图流水线节点，内部串行执行“普通图提示词 Agent -> 普通图生成”。
 */
@Component
public class RednoteNormalImagePipelineNodeHandler implements NodeAction {

    private final RednoteNormalImagePromptAgent normalImagePromptAgent;

    private final RednoteNormalImageGenerateNodeHandler normalImageGenerateNodeHandler;

    public RednoteNormalImagePipelineNodeHandler(RednoteNormalImagePromptAgent normalImagePromptAgent,
            RednoteNormalImageGenerateNodeHandler normalImageGenerateNodeHandler) {
        this.normalImagePromptAgent = normalImagePromptAgent;
        this.normalImageGenerateNodeHandler = normalImageGenerateNodeHandler;
    }

    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) throws Exception {
        Map<String, Object> updates = new LinkedHashMap<>();
        Map<String, Object> promptUpdates = runAgentNode(normalImagePromptAgent.asNode(), stateSnapshot);
        updates.putAll(promptUpdates);

        Map<String, Object> nextState = new LinkedHashMap<>(stateSnapshot.data());
        nextState.putAll(promptUpdates);
        Map<String, Object> imageUpdates = normalImageGenerateNodeHandler.apply(new OverAllState(nextState));
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
