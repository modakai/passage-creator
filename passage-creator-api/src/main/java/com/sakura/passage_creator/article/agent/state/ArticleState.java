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


}
