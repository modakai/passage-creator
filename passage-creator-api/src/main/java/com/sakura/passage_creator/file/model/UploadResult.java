package com.sakura.passage_creator.file.model;

import lombok.Builder;
import lombok.Data;

/**
 * 上传返回体
 *
 * @author sakura
 */
@Data
@Builder
public class UploadResult {

    /**
     * 文件路径
     */
    private String url;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 已上传对象的实体标记，用于校验文件
     */
    private String eTag;
}
