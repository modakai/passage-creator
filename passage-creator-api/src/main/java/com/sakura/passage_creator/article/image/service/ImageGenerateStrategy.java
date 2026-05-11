package com.sakura.passage_creator.article.image.service;

/**
 * 配图生成策略接口，隔离图库检索、图表渲染、AI 生图等不同来源。
 */
public interface ImageGenerateStrategy extends ImageTool {
}
