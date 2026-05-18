package com.sakura.passage_creator.rednote.workflow;

import lombok.Getter;

/**
 * 小红书爆文创作 workflow node 类型
 *
 * @author sakura
 * @create 2026-05
 */
@Getter
public enum RednoteWorkflowNodeType {

    // 节点1：根据用户输入的词，去搜索网页搜索对应的内容
    SEARCH_AGENT("SEARCH_AGENT"),
    // 节点2：根据搜索到的主题内容，生成对应的文案
    COPY_GENERATING("COPY_GENERATING"),
    // 节点3：生成正文的图片提示词：配图提示词、封面提示词
    IMAGE_PROMPT_GENERATING("IMAGE_PROMPT_GENERATING"),
    // 图片提示词分支汇总节点
    IMAGE_PROMPT_COMPLETED("IMAGE_PROMPT_COMPLETED"),
    // 节点4：并行生成图片
    IMAGE_GENERATING("IMAGE_GENERATING"),
    // 普通配图生成分支节点
    NORMAL_IMAGE_GENERATING("NORMAL_IMAGE_GENERATING"),
    // 封面图生成分支节点
    COVER_IMAGE_GENERATING("COVER_IMAGE_GENERATING"),
    // 普通配图子图节点，内部串行执行普通图提示词和图片生成
    NORMAL_IMAGE_PIPELINE("NORMAL_IMAGE_PIPELINE"),
    // 封面图子图节点，内部串行执行封面提示词和封面图片生成
    COVER_IMAGE_PIPELINE("COVER_IMAGE_PIPELINE"),
    // 普通配图和封面图都结束后的完成节点
    COMPLETED("COMPLETED");

    /**
     * StateGraph 持久化节点值。
     */
    private final String value;

    RednoteWorkflowNodeType(String value) {
        this.value = value;
    }
}
