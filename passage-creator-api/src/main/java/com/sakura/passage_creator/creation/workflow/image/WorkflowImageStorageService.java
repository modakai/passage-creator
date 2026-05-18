package com.sakura.passage_creator.creation.workflow.image;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.sakura.passage_creator.infrastructure.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * workflow 通用图片存储服务，负责把生成出的图片字节上传到 OSS。
 */
@Service
@Slf4j
public class WorkflowImageStorageService {

    /**
     * OSS 客户端。
     */
    private final OSS ossClient;

    /**
     * OSS 配置属性。
     */
    private final OssConfig.OssProperties ossProperties;

    public WorkflowImageStorageService(OSS ossClient, OssConfig.OssProperties ossProperties) {
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
    public String uploadArticleImage(String taskId, WorkflowImageData imageData) {
        return uploadGeneratedImage("article", taskId, imageData);
    }

    /**
     * 上传生成图片并返回公开访问地址，bizType 用于区分 article、rednote 等业务目录。
     *
     * @param bizType   业务类型目录
     * @param taskId    任务 id
     * @param imageData 图片数据
     * @return 图片 URL
     */
    public String uploadGeneratedImage(String bizType, String taskId, WorkflowImageData imageData) {
        if (imageData == null || !imageData.isValid()) {
            throw new IllegalArgumentException("图片数据无效，无法上传 OSS");
        }

        String objectName = buildObjectName(bizType, taskId, imageData.getExtension());
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
        log.info("workflow 图片上传成功, bizType={}, taskId={}, url={}", bizType, taskId, url);
        return url;
    }

    /**
     * 构造图片对象名，按业务类型和任务隔离便于排查和清理。
     */
    private String buildObjectName(String bizType, String taskId, String extension) {
        String prefix = ossProperties.getPrefix() == null ? "" : ossProperties.getPrefix().replaceAll("/+$", "");
        String ext = extension == null || extension.isBlank() ? ".png" : extension;
        String normalizedPrefix = prefix.isBlank() ? "" : prefix + "/";
        String safeBizType = bizType == null || bizType.isBlank() ? "generated" : bizType.replaceAll("[^a-zA-Z0-9_-]", "");
        return "%s%s/%s/%s%s".formatted(normalizedPrefix, safeBizType, taskId, UUID.randomUUID(), ext);
    }

    /**
     * 构造 OSS 公开访问地址，保持和现有上传服务一致。
     */
    private String buildPublicUrl(String objectName) {
        return String.format("https://%s.%s/%s", ossProperties.getBucketName(), ossProperties.getEndpoint(), objectName);
    }
}
