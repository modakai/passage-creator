package com.sakura.passage_creator.rednote.workflow.state;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
     * 任务id
     */
    private String taskId;

    /**
     * 用户输入的内容
     */
    private String input;

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

    public static record SearchResponse(
            //主题
            String subject,
            // 检索到的上下文
            String context,
            // 正文字数
            String contentLength,
            // 目标字数
            Integer targetWordCount,
            // 关键字
            List<String> keywords,
            // 标签个数
            Integer tagCount,
            // 图片个数
            Integer imageCount,
            List<SearchResult> searchResults
    ) {
    }

    public static record SearchResult(
            String title,
            String summary,
            String sourceName,
            String sourceUrl
    ) {
    }

    public static record ImagePromptResponse(
            String coverPrompt,
            String imagePrompts
    ) {
    }
}

