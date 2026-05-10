package com.sakura.passage_creator.article.image.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.sakura.passage_creator.article.model.dto.image.ImageData;
import com.sakura.passage_creator.infrastructure.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * 文章配图存储服务，负责把生成出的图片字节上传到 OSS。
 */
@Service
@Slf4j
public class ArticleImageStorageService {

    /**
     * OSS 客户端。
     */
    private final OSS ossClient;

    /**
     * OSS 配置属性。
     */
    private final OssConfig.OssProperties ossProperties;

    public ArticleImageStorageService(OSS ossClient, OssConfig.OssProperties ossProperties) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 上传文章配图并返回公开访问地址。
     *
     * @param taskId    文章任务 id
     * @param imageData 图片数据
     * @return 图片 URL
     */
    public String uploadArticleImage(String taskId, ImageData imageData) {
        if (imageData == null || !imageData.isValid()) {
            throw new IllegalArgumentException("图片数据无效，无法上传 OSS");
        }

        String objectName = buildObjectName(taskId, imageData.getExtension());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(imageData.getMimeType());
        metadata.setContentLength(imageData.getBytes().length);

        PutObjectRequest request = new PutObjectRequest(
                ossProperties.getBucketName(),
                objectName,
                new ByteArrayInputStream(imageData.getBytes()),
                metadata
        );
        ossClient.putObject(request);

        String url = buildPublicUrl(objectName);
        log.info("文章配图上传成功, taskId={}, url={}", taskId, url);
        return url;
    }

    /**
     * 构造文章配图对象名，按任务隔离便于排查和清理。
     */
    private String buildObjectName(String taskId, String extension) {
        String prefix = ossProperties.getPrefix() == null ? "" : ossProperties.getPrefix().replaceAll("/+$", "");
        String ext = extension == null || extension.isBlank() ? ".png" : extension;
        String normalizedPrefix = prefix.isBlank() ? "" : prefix + "/";
        return "%sarticle/%s/%s%s".formatted(normalizedPrefix, taskId, UUID.randomUUID(), ext);
    }

    /**
     * 构造 OSS 公开访问地址，保持和现有上传服务一致。
     */
    private String buildPublicUrl(String objectName) {
        return String.format("https://%s.%s/%s", ossProperties.getBucketName(), ossProperties.getEndpoint(), objectName);
    }
}
