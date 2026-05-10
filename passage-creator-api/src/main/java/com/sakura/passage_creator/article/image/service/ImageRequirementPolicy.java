package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 配图需求策略，负责第一阶段的数量控制和配图方式收敛。
 */
public class ImageRequirementPolicy {

    private static final int COVER_POSITION = 1;

    /**
     * 除封面外最多保留的章节配图数量。
     */
    private final int maxSectionImages;

    /**
     * 创建配图需求策略。
     *
     * @param maxSectionImages 除封面外最多保留的章节配图数量
     */
    public ImageRequirementPolicy(int maxSectionImages) {
        this.maxSectionImages = Math.max(0, maxSectionImages);
    }

    /**
     * 对模型输出的配图需求进行裁剪，并强制改为 GPT_IMAGE。
     *
     * @param requirements 原始配图需求
     * @return 可执行的配图需求
     */
    public List<ArticleState.ImageRequirement> apply(List<ArticleState.ImageRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }

        List<ArticleState.ImageRequirement> sorted = requirements.stream()
                .filter(item -> item != null && item.getPosition() != null)
                .sorted(Comparator.comparing(ArticleState.ImageRequirement::getPosition))
                .toList();

        List<ArticleState.ImageRequirement> selected = new ArrayList<>();
        sorted.stream()
                .filter(this::isCover)
                .findFirst()
                .ifPresent(selected::add);

        sorted.stream()
                .filter(item -> !isCover(item))
                .limit(maxSectionImages)
                .forEach(selected::add);

        selected.forEach(item -> item.setImageSource(ImageMethodEnum.GPT_IMAGE.getValue()));
        return selected;
    }

    /**
     * 判断当前需求是否为封面图。
     */
    private boolean isCover(ArticleState.ImageRequirement requirement) {
        return COVER_POSITION == requirement.getPosition()
                || "cover".equalsIgnoreCase(requirement.getType());
    }
}
