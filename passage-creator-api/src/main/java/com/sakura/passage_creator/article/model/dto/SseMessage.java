package com.sakura.passage_creator.article.model.dto;

import com.sakura.passage_creator.article.model.enums.SseMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章生成 SSE 消息体。
 *
 * @author sakura
 * @create 2026-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage<T> {

    /**
     * 消息类型。
     */
    private String type;

    /**
     * 消息描述。
     */
    private String message;

    /**
     * 消息数据。
     */
    private T data;

    /**
     * 创建标准 SSE 消息体。
     *
     * @param type 消息类型枚举
     * @param data 消息数据
     * @return SSE 消息体
     * @param <T> 数据类型
     */
    public static <T> SseMessage<T> of(SseMessageTypeEnum type, T data) {
        return new SseMessage<>(type.getValue(), type.getDescription(), data);
    }
}
