package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;

/**
 * 配图需求文本解析器，统一决定不同策略优先使用 prompt 还是 keywords。
 */
final class ImageRequirementTextResolver {

    private ImageRequirementTextResolver() {
    }

    /**
     * 获取策略执行参数：AI/图表类优先 prompt，检索类优先 keywords。
     */
    static String resolve(ArticleState.ImageRequirement requirement, ImageMethodEnum method) {
        if (method.isAiGenerated()) {
            return StrUtil.blankToDefault(requirement.getPrompt(), requirement.getKeywords());
        }
        return StrUtil.blankToDefault(requirement.getKeywords(), requirement.getPrompt());
    }
}
