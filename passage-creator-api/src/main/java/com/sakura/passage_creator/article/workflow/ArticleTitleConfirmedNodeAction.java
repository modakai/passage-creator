package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 标题确认节点动作，人工确认结果由恢复 workflow 前写入 Graph checkpoint。
 */
@Component
public class ArticleTitleConfirmedNodeAction implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 该节点不做业务写入，只把 checkpoint 中的人工选择显式传给后续大纲节点。
        return Map.of(
                "selectedMainTitle", state.value("selectedMainTitle", ""),
                "selectedSubTitle", state.value("selectedSubTitle", ""),
                "userDescription", state.value("userDescription", "")
        );
    }
}
