package com.sakura.passage_creator.rednote.workflow;

/**
 * 小红书爆文创作 workflow node 类型
 *
 * @author sakura
 * @create 2026-05
 */
public enum RednoteWorkflowNodeType {

    // 节点1：根据用户输入的词，去搜索网页搜索对应的内容
    SEARCH_AGENT("SEARCH_AGENT"),
    // 节点2：根据搜索到的主题内容，生成对应的文案
    COPY_GENERATING("COPY_GENERATING"),
    // 节点3：生成正文的图片提示词：配图提示词、封面提示词
    IMAGE_PROMPT_GENERATING("IMAGE_PROMPT_GENERATING"),
    // 节点4：并行生成图片
    IMAGE_GENERATING("IMAGE_GENERATING");

    private final String value;

    RednoteWorkflowNodeType(String value) {
        this.value = value;
    }
}
