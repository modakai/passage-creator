package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 配图工具注册表，负责按 ImageMethodEnum 查找可执行工具。
 */
@Component
public class ImageToolRegistry {

    private final Map<ImageMethodEnum, ImageTool> toolMap = new EnumMap<>(ImageMethodEnum.class);

    public ImageToolRegistry(List<ImageTool> imageTools) {
        for (ImageTool imageTool : imageTools) {
            toolMap.put(imageTool.getMethod(), imageTool);
        }
    }

    /**
     * 获取指定配图方式对应的工具，缺失或不可用时抛出清晰异常。
     */
    public ImageTool getRequiredTool(ImageMethodEnum method) {
        ImageTool imageTool = toolMap.get(method);
        if (imageTool == null || !imageTool.isAvailable()) {
            throw new IllegalStateException("配图工具不可用: " + method.getValue());
        }
        return imageTool;
    }
}
