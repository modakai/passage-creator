package com.sakura.passage_creator.creation.workflow.image;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * workflow 通用远程图片下载器，用于处理图片模型或图库返回的临时 URL。
 */
@Component
public class WorkflowRemoteImageDownloader {

    private static final String DEFAULT_MIME_TYPE = "image/png";

    private static final String DEFAULT_EXTENSION = ".png";

    private final RestClient restClient = RestClient.builder().build();

    /**
     * 下载远程图片并转换为可上传 OSS 的图片数据。
     *
     * @param imageUrl 远程图片 URL
     * @return 图片二进制数据
     */
    public WorkflowImageData download(String imageUrl) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .toEntity(byte[].class);
        byte[] bytes = response.getBody();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("远程图片下载结果为空");
        }

        String mimeType = resolveMimeType(response);
        return WorkflowImageData.builder()
                .bytes(bytes)
                .mimeType(mimeType)
                .extension(resolveExtension(mimeType))
                .build();
    }

    /**
     * 从响应头中识别 MIME 类型，缺失时按 PNG 兜底。
     */
    private String resolveMimeType(ResponseEntity<byte[]> response) {
        MediaType contentType = response.getHeaders().getContentType();
        return contentType == null ? DEFAULT_MIME_TYPE : contentType.toString();
    }

    /**
     * 根据 MIME 类型选择 OSS 对象扩展名。
     */
    private String resolveExtension(String mimeType) {
        String normalizedMimeType = mimeType == null ? "" : mimeType.toLowerCase();
        if (normalizedMimeType.startsWith("image/jpeg") || normalizedMimeType.startsWith("image/jpg")) {
            return ".jpg";
        }
        if (normalizedMimeType.startsWith("image/webp")) {
            return ".webp";
        }
        if (normalizedMimeType.startsWith("image/svg+xml")) {
            return ".svg";
        }
        return DEFAULT_EXTENSION;
    }
}
