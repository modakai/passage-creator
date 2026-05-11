package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;

/**
 * 配图工具接口，作为配图 Agent 调用外部能力的统一边界。
 */
public interface ImageTool {

    /**
     * 当前工具支持的配图方式。
     */
    ImageMethodEnum getMethod();

    /**
     * 执行工具并返回可上传 OSS 的图片结果。
     *
     * @param requirement 配图需求
     * @return 图片生成结果
     */
    ImageGenerationResult generate(ArticleState.ImageRequirement requirement);

    /**
     * 判断工具是否具备运行条件，例如 API Key 或远程服务配置。
     */
    default boolean isAvailable() {
        return true;
    }
}
