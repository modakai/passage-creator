package com.sakura.passage_creator.file.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.sakura.passage_creator.infrastructure.config.OssConfig;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@ConditionalOnProperty(value = "oss.enable", havingValue = "true")
public class OssService {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    /**
     * OSS 客户端。
     */
    private final OSS ossClient;

    /**
     * OSS 配置属性。
     */
    private final OssConfig.OssProperties ossProperties;

    public OssService(OSS ossClient, OssConfig.OssProperties ossProperties) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 通用上传方法：上传文件并返回可访问的 URL
     *
     * @param uploadFile 要上传的文件
     * @param objectName 上传到 OSS 的对象名，例如 `images/2025/10/avatar.png`
     * @return 可公开访问的 URL
     */
    public String uploadFile(File uploadFile, String objectName) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), objectName, uploadFile);
            // 执行上传
            return upload(objectName, putObjectRequest);
        } catch (Exception e) {
            log.error("OSS 文件上传失败，objectName: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 通用上传方法：上传文件并返回可访问的 URL
     *
     * @param file 要上传的文件，Spring 的 MultipartFile
     * @return 可公开访问的 URL
     */
    public String uploadFile(MultipartFile file) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        if (loginUser == null || loginUser.userId() == null) {
            throw new RuntimeException("未获取到登录用户信息，上传失败");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("文件名为空，上传失败");
        }
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String objectName = String.format( ossProperties.getPrefix() + "/%d/%s/%s", loginUser.userId(), datePath, originalFilename);
        try (InputStream inputStream = file.getInputStream()) {
            // 构造上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), objectName, inputStream);
            // 执行上传
            return upload(objectName, putObjectRequest);
        } catch (Exception e) {
            log.error("OSS 文件上传失败，objectName: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 重载方法：支持直接传入 InputStream 和 contentType
     */
    public String uploadFile(InputStream inputStream, String objectName, String contentType) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), objectName, inputStream);
            if (contentType != null && !contentType.isEmpty()) {
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentType(contentType);
                putObjectRequest.setMetadata(objectMetadata);
            }
            return upload(objectName, putObjectRequest);
        } catch (Exception e) {
            log.error("OSS 文件上传失败，objectName: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    private String upload(String objectName, PutObjectRequest putObjectRequest) {
        ossClient.putObject(putObjectRequest);
        String url = String.format("https://%s.%s/%s", ossProperties.getBucketName(), ossProperties.getEndpoint(), objectName);
        log.info("文件上传成功，访问地址: {}", url);
        return url;
    }
}
