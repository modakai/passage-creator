package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 配图需求策略，负责数量控制和配图方式合法化。
 */
public class ImageRequirementPolicy {

    private static final int COVER_POSITION = 1;

    /**
     * 除封面外最多保留的章节配图数量。
     */
    private final int maxSectionImages;

    /**
     * 用户当前允许使用的配图方式，不包含系统降级方式。
     */
    private final List<ImageMethodEnum> allowedMethods;

    /**
     * 创建配图需求策略。
     *
     * @param maxSectionImages 除封面外最多保留的章节配图数量
     */
    public ImageRequirementPolicy(int maxSectionImages) {
        this(maxSectionImages, null);
    }

    /**
     * 创建带用户允许方式约束的配图需求策略。
     *
     * @param maxSectionImages 除封面外最多保留的章节配图数量
     * @param allowedMethodValues 用户允许的配图方式值
     */
    public ImageRequirementPolicy(int maxSectionImages, List<String> allowedMethodValues) {
        this.maxSectionImages = Math.max(0, maxSectionImages);
        this.allowedMethods = resolveAllowedMethods(allowedMethodValues);
    }

    /**
     * 对模型输出的配图需求进行裁剪，并把未知或未授权方式降级为用户允许的默认方式。
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

        selected.forEach(this::normalizeImageSource);
        return selected;
    }

    /**
     * 只接受后端已注册的配图方式，避免模型幻觉输出旧的 NANO_BANANA 等来源。
     */
    private void normalizeImageSource(ArticleState.ImageRequirement requirement) {
        ImageMethodEnum method = ImageMethodEnum.getByValue(requirement.getImageSource());
        if (method == null || method.isFallback() || !allowedMethods.contains(method)) {
            requirement.setImageSource(resolveDefaultAllowedMethod().getValue());
        }
        else {
            requirement.setImageSource(method.getValue());
        }
    }

    /**
     * 解析用户允许的方式；为空时默认开放所有非降级配图方式，保持兼容。
     */
    private List<ImageMethodEnum> resolveAllowedMethods(List<String> allowedMethodValues) {
        if (allowedMethodValues == null || allowedMethodValues.isEmpty()) {
            return ImageMethodEnum.userSelectableMethods();
        }
        List<ImageMethodEnum> methods = allowedMethodValues.stream()
                .map(ImageMethodEnum::getByValue)
                .filter(method -> method != null && !method.isFallback())
                .distinct()
                .toList();
        return methods.isEmpty() ? List.of(ImageMethodEnum.getDefaultAiMethod()) : methods;
    }

    /**
     * 默认优先使用 GPT_IMAGE；如果用户未允许 GPT_IMAGE，则使用用户允许列表中的第一项。
     */
    private ImageMethodEnum resolveDefaultAllowedMethod() {
        if (allowedMethods.contains(ImageMethodEnum.getDefaultAiMethod())) {
            return ImageMethodEnum.getDefaultAiMethod();
        }
        return allowedMethods.get(0);
    }

    /**
     * 判断当前需求是否为封面图。
     */
    private boolean isCover(ArticleState.ImageRequirement requirement) {
        return COVER_POSITION == requirement.getPosition()
                || "cover".equalsIgnoreCase(requirement.getType());
    }
}
