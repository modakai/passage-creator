package com.sakura.passage_creator.article.agent.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Agent中的状态
 *
 * @author sakura
 * @create 2026-04
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 选题
     */
    private String topic;

    /**
     * 用户补充描述
     */
    private String userDescription;

    /**
     * 文章风格
     */
    private String style;

    /**
     * 当前阶段
     */
    private String phase;

    /**
     * 标题方案列表（标题智能体输出）
     */
    private List<TitleOption> titleOptions;

    /**
     * 标题结果（标题智能体 {@link com.sakura.passage_creator.article.agent.TitleGeneratorAgent} 输出）
     */
    private TitleResult title;

    /**
     * 大纲结果（智能体2输出）
     */
    private OutlineResult outline;

    /**
     * 正文结果（智能体3输出），使用 Markdown 格式。
     */
    private String content;

    /**
     * 配图需求列表，由配图分析 Agent 根据正文和配图数量上限生成。
     */
    private List<ImageRequirement> imageRequirements;

    /**
     * 配图结果列表，保存生成后的公开访问地址和占位符映射。
     */
    private List<ImageResult> images;

    /**
     * 完整图文内容，使用 Markdown 格式并包含已替换的配图链接。
     */
    private String fullContent;

    /**
     * 封面图 URL，封面不插入正文，但用于列表或详情头图展示。
     */
    private String coverImage;

    /**
     * 允许使用的配图方式，后续接入 VIP 或用户选择时用于约束 ImageAnalyzerAgent。
     */
    private List<String> enabledImageMethods;


    /**
     * 标题方案
     */
    @Data
    public static class TitleOption implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 标题结果
     */
    @Data
    public static class TitleResult implements Serializable {
        private String mainTitle;
        private String subTitle;
    }

    /**
     * 大纲结果
     */
    @Data
    public static class OutlineResult implements Serializable {
        private List<OutlineSection> sections;
    }

    /**
     * 大纲章节
     */
    @Data
    public static class OutlineSection implements Serializable {
        private Integer section;
        private String title;
        private List<String> points;
    }

    /**
     * 配图需求，描述一张图片应该如何生成以及放在正文哪里。
     */
    @Data
    public static class ImageRequirement implements Serializable {
        private Integer position;
        private String type;
        private String sectionTitle;
        private String imageSource;
        private String keywords;
        private String prompt;
        private String placeholderId;
    }

    /**
     * 配图结果，保存图片 URL 和正文占位符之间的对应关系。
     */
    @Data
    public static class ImageResult implements Serializable {
        private Integer position;
        private String url;
        private String method;
        private String keywords;
        private String sectionTitle;
        private String description;
        private String placeholderId;
    }

    /**
     * 配图分析 Agent 的结构化返回值，包含带占位符正文和配图需求。
     */
    @Data
    public static class Agent4Result implements Serializable {
        private String contentWithPlaceholders;
        private List<ImageRequirement> imageRequirements;
    }

}
