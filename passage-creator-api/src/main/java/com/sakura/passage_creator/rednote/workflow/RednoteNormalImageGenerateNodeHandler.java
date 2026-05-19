package com.sakura.passage_creator.rednote.workflow;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.service.RednoteImageGenerationService;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 普通配图生成节点，基于普通图片提示词并行生成最多 5 张配图。
 */
@Component
public class RednoteNormalImageGenerateNodeHandler implements NodeAction {

    private final RednoteImageGenerationService rednoteImageGenerationService;

    private final RednoteNotePersistenceService rednoteNotePersistenceService;

    public RednoteNormalImageGenerateNodeHandler(RednoteImageGenerationService rednoteImageGenerationService,
                                                 RednoteNotePersistenceService rednoteNotePersistenceService) {
        this.rednoteImageGenerationService = rednoteImageGenerationService;
        this.rednoteNotePersistenceService = rednoteNotePersistenceService;
    }

    /**
     * 普通配图在节点内部并行执行，避免最多 5 张图串行拖慢 workflow。
     */
    @Override
    public Map<String, Object> apply(OverAllState stateSnapshot) {
        String taskId = requiredString(stateSnapshot, RednoteWorkflowState.KEY_TASK_ID);
        Long userId = requiredLong(stateSnapshot, RednoteWorkflowState.KEY_USER_ID);
        rednoteNotePersistenceService.markPhase(taskId,
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_GENERATING.getValue());
        List<RednoteWorkflowState.ImagePromptItem> promptItems = resolvePromptItems(stateSnapshot);
        List<CompletableFuture<RednoteWorkflowState.RednoteImageResult>> futures = promptItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> rednoteImageGenerationService.generate(
                        taskId,
                        userId,
                        item.getPrompt(),
                        item.getPosition(),
                        "NORMAL"
                )))
                .toList();
        List<RednoteWorkflowState.RednoteImageResult> images = futures.stream()
                .map(CompletableFuture::join)
                .sorted(Comparator.comparing(RednoteWorkflowState.RednoteImageResult::getPosition))
                .toList();
        rednoteNotePersistenceService.saveNormalImages(taskId,
                JSONUtil.toJsonStr(images),
                RednoteStatusEnum.PROCESSING.getValue(),
                RednotePhaseEnum.IMAGE_GENERATING.getValue());
        return Map.of(RednoteWorkflowState.KEY_IMAGES, images);
    }

    /**
     * 从 state 中读取普通配图提示词并反序列化为强类型列表。
     */
    private List<RednoteWorkflowState.ImagePromptItem> resolvePromptItems(OverAllState stateSnapshot) {
        Object value = stateSnapshot.value(RednoteWorkflowState.KEY_IMAGE_PROMPTS)
                .orElseThrow(() -> new IllegalStateException("缺少普通配图提示词"));
        List<RednoteWorkflowState.ImagePromptItem> items =
                JSONUtil.toList(JSONUtil.toJsonStr(value), RednoteWorkflowState.ImagePromptItem.class);
        return items.stream()
                .filter(item -> item != null && StringUtils.isNotBlank(item.getPrompt()))
                .limit(5)
                .toList();
    }

    /**
     * 读取必填字符串状态。
     */
    private String requiredString(OverAllState stateSnapshot, String key) {
        Object value = stateSnapshot.value(key).orElse(null);
        if (value == null || StringUtils.isBlank(value.toString())) {
            throw new IllegalStateException("缺少 rednote workflow 状态：" + key);
        }
        return value.toString();
    }

    /**
     * 读取必填 Long 状态。
     */
    private Long requiredLong(OverAllState stateSnapshot, String key) {
        Object value = stateSnapshot.value(key).orElse(null);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && StringUtils.isNotBlank(value.toString())) {
            return Long.parseLong(value.toString());
        }
        throw new IllegalStateException("缺少 rednote workflow 状态：" + key);
    }
}
