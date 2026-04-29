package com.sakura.passage_creator.article.agent.state;

import lombok.Data;

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


}
