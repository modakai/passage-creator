package com.sakura.passage_creator.rednote.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流中 上下文传递的状态
 *
 * @author sakura
 * @create 2026-05
 */
@Getter
@Setter
@Accessors(chain = true)
public class RednoteWorkflowState {

    /**
     * 搜索结果的输出
     */
    public static final String SEARCH_OUTPUT_KEY = "brief";

    /**
     * workflow 任务 id 状态键。
     */
    public static final String KEY_TASK_ID = "taskId";

    /**
     * 创建用户 id 状态键。
     */
    public static final String KEY_USER_ID = "userId";

    /**
     * 用户原始自然语言创作需求状态键。
     */
    public static final String KEY_CONTENT = "content";

    /**
     * SearchAgent 结构化简报状态键。
     */
    public static final String KEY_SEARCH_RESPONSE = "searchResponse";

    /**
     * SearchAgent 解析出的主体状态键。
     */
    public static final String KEY_SUBJECT = "subject";

    /**
     * SearchAgent 整理后的上下文状态键。
     */
    public static final String KEY_CONTEXT = "context";

    /**
     * 篇幅档位状态键。
     */
    public static final String KEY_CONTENT_LENGTH = "contentLength";

    /**
     * 目标字数状态键。
     */
    public static final String KEY_TARGET_WORD_COUNT = "targetWordCount";

    /**
     * 关键词列表状态键。
     */
    public static final String KEY_KEYWORDS = "keywords";

    /**
     * 标签数量状态键。
     */
    public static final String KEY_TAG_COUNT = "tagCount";

    /**
     * 普通配图数量状态键。
     */
    public static final String KEY_IMAGE_COUNT = "imageCount";

    /**
     * 搜索结果摘要状态键。
     */
    public static final String KEY_SEARCH_RESULTS = "searchResults";

    /**
     * 文案生成结果状态键。
     */
    public static final String KEY_COPYWRITING = "copywriting";

    /**
     * 封面提示词状态键。
     */
    public static final String KEY_COVER_PROMPT = "coverPrompt";

    /**
     * 普通配图提示词状态键。
     */
    public static final String KEY_IMAGE_PROMPTS = "imagePrompts";

    /**
     * 普通配图提示词 Agent 原始结构化输出状态键。
     */
    public static final String KEY_NORMAL_IMAGE_PROMPT_RESPONSE = "normalImagePromptResponse";

    /**
     * 封面图提示词 Agent 原始结构化输出状态键。
     */
    public static final String KEY_COVER_IMAGE_PROMPT_RESPONSE = "coverImagePromptResponse";

    /**
     * 普通配图结果状态键。
     */
    public static final String KEY_IMAGES = "images";

    /**
     * 封面图生成结果状态键。
     */
    public static final String KEY_COVER_IMAGE_RESULT = "coverImageResult";

    /**
     * 封面图结果状态键。
     */
    public static final String KEY_COVER_IMAGE = "coverImage";

    /**
     * 标签列表状态键。
     */
    public static final String KEY_TAGS = "tags";

    /**
     * 小红书正文主体状态键。
     */
    public static final String KEY_BODY_CONTENT = "bodyContent";

    /**
     * 封面标题状态键，由后续图片提示词节点写入。
     */
    public static final String KEY_COVER_TITLE = "coverTitle";

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 用户输入的内容
     */
    private String content;

    /**
     * 节点1的搜索结果
     */
    private SearchResponse searchResponse;

    /**
     * 小红书正文
     */
    private String bodyContent;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 图片生成提示词
     */
    private ImagePromptResponse imagePromptResponse;

    /**
     * 封面标题
     */
    private String coverTitle;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 配图列表
     */
    private List<String> images;

    /**
     * SearchAgent 输出的结构化创作简报。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponse implements Serializable {

        /**
         * 核心主体、产品或场景。
         */
        private String subject;

        /**
         * 检索结果和用户意图整合后的创作上下文。
         */
        private String context;

        /**
         * 篇幅档位：SHORT/MEDIUM/LONG。
         */
        private String contentLength;

        /**
         * 目标字数。
         */
        private Integer targetWordCount;

        /**
         * 关键词列表。
         */
        private List<String> keywords;

        /**
         * 标签个数。
         */
        private Integer tagCount;

        /**
         * 普通图片个数，最多 5 张，不含封面。
         */
        private Integer imageCount;

        /**
         * 搜索结果摘要列表。
         */
        private List<SearchResult> searchResults;
    }

    /**
     * SearchAgent 保存的单条搜索摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult implements Serializable {

        /**
         * 搜索结果标题。
         */
        private String title;

        /**
         * 可用于创作的摘要。
         */
        private String summary;

        /**
         * 来源名称。
         */
        private String sourceName;

        /**
         * 来源链接。
         */
        private String sourceUrl;
    }

    /**
     * 文案 Agent 输出的结构化小红书内容。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentResponse implements Serializable {

        /**
         * 可直接发布的小红书内容，不包含标签。
         */
        private String bodyContent;

        /**
         * 标签建议列表，保存时会序列化到 rednote_note.tags。
         */
        private List<String> tags;
    }

    /**
     * 图片提示词 Agent 输出结构。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagePromptResponse implements Serializable {

        /**
         * 封面图片提示词。
         */
        private String coverPrompt;

        /**
         * 普通配图提示词 JSON 文本。
         */
        private String imagePrompts;
    }

    /**
     * 普通图片提示词 Agent 输出结构。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NormalImagePromptResponse implements Serializable {

        /**
         * 普通配图提示词列表，最多 5 条。
         */
        private List<ImagePromptItem> imagePrompts;
    }

    /**
     * 单张普通配图的提示词计划。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagePromptItem implements Serializable {

        /**
         * 图片顺序，从 1 开始。
         */
        private Integer position;

        /**
         * 图片用途说明，例如“步骤图”“氛围图”。
         */
        private String purpose;

        /**
         * 最终用于图片生成的主提示词。
         */
        private String prompt;

        /**
         * 轻微差异化的备选提示词。
         */
        private List<String> variants;
    }

    /**
     * 封面图提示词 Agent 输出结构。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoverImagePromptResponse implements Serializable {

        /**
         * 封面图主标题，严格不超过 12 个字。
         */
        private String title;

        /**
         * 封面图副标题，控制在 10-20 个字。
         */
        private String subtitle;

        /**
         * 封面图点缀文案，控制在 10-20 个字。
         */
        private String decorativeText;

        /**
         * 封面图相关标签，通常为 4-5 个。
         */
        private List<String> tags;

        /**
         * 最终用于生成封面图的提示词。
         */
        private String coverPrompt;
    }

    /**
     * Rednote 图片生成结果，保存 URL、提示词和生成状态。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RednoteImageResult implements Serializable {

        /**
         * 图片顺序，普通配图从 1 开始，封面图固定为 0。
         */
        private Integer position;

        /**
         * 图片类型：NORMAL 或 COVER。
         */
        private String type;

        /**
         * 图片公开访问 URL。
         */
        private String url;

        /**
         * 本次图片生成使用的提示词。
         */
        private String prompt;

        /**
         * 本次图片生成使用的模型。
         */
        private String model;

        /**
         * 单张图片生成状态：SUCCESS/FAILED。
         */
        private String status;

        /**
         * 单张图片失败原因。
         */
        private String errorMessage;
    }
}
