package com.sakura.passage_creator.creation.workflow.image;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * workflow 通用图片二进制数据，作为图片生成服务和存储服务之间的边界对象。
 */
@Data
@Builder
public class WorkflowImageData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图片原始字节。
     */
    private byte[] bytes;

    /**
     * 图片 MIME 类型，例如 image/png。
     */
    private String mimeType;

    /**
     * 文件扩展名，包含点号，例如 .png。
     */
    private String extension;

    /**
     * 判断图片数据是否具备上传条件。
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return bytes != null && bytes.length > 0 && mimeType != null && !mimeType.isBlank();
    }
}
