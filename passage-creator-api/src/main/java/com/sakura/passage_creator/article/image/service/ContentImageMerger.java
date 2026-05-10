package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图文合成服务，负责把正文中的图片占位符替换为 Markdown 图片语法。
 */
@Component
public class ContentImageMerger {

    /**
     * 将配图结果合并进正文。封面图没有占位符，因此不会被插入正文。
     *
     * @param content 带占位符的 Markdown 正文
     * @param images  配图结果列表
     * @return 合成后的完整 Markdown
     */
    public String merge(String content, List<ArticleState.ImageResult> images) {
        if (StrUtil.isBlank(content) || images == null || images.isEmpty()) {
            return content;
        }

        String fullContent = content;
        for (ArticleState.ImageResult image : images) {
            if (image == null || StrUtil.isBlank(image.getPlaceholderId()) || StrUtil.isBlank(image.getUrl())) {
                continue;
            }
            String description = StrUtil.blankToDefault(image.getDescription(), "配图");
            String markdownImage = "![" + description + "](" + image.getUrl() + ")";
            // Agent 可能返回裸占位符 ID，但正文里使用 {{ID}}，因此先替换包裹形式，避免残留花括号。
            String placeholderId = image.getPlaceholderId();
            fullContent = fullContent.replace("{{" + placeholderId + "}}", markdownImage);
            fullContent = fullContent.replace(placeholderId, markdownImage);
        }
        return fullContent;
    }
}
