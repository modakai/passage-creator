package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 大纲确认节点动作，人工确认后的大纲由恢复 workflow 前写入 Graph checkpoint。
 */
@Component
public class ArticleOutlineConfirmedNodeAction implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> updates = new HashMap<>();
        // confirmedOutline 可能来自复杂 JSON，保持原对象形态让正文节点再做强类型转换。
        state.value("confirmedOutline").ifPresent(outline -> updates.put("confirmedOutline", outline));
        return updates;
    }
}
