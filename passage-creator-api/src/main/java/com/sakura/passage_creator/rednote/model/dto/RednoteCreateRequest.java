package com.sakura.passage_creator.rednote.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 小红书爆款笔记创建请求。
 */
@Data
public class RednoteCreateRequest implements Serializable {

    /**
     * 用户原始自然语言创作需求，可包含主题、字数、关键词、标签数和图片数量等描述。
     */
    @NotBlank(message = "小红书创作需求不能为空")
    private String content;

    private static final long serialVersionUID = 1L;
}
