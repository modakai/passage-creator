package com.sakura.passage_creator.infrastructure.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * OSS 配置
 *
 * @author sakura
 */
@Configuration
@ConditionalOnProperty(value = "oss.enable")
public class OssConfig {

    /**
     * 阿里云 OSS 客户端
     *
     * @param aliOssProperties 配置类
     * @return OSS 客户端
     */
    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(OssProperties aliOssProperties) {
        String endpoint = aliOssProperties.getEndpoint();
        String region = aliOssProperties.getRegion();
        String accessKey = aliOssProperties.getAccessKey();
        String secretKey = aliOssProperties.getSecretKey();
        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(accessKey, secretKey);
        return OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
    }

    /**
     * OSS 对象存储配置属性
     *
     * @author sakura
     */
    @Data
    @Component
    @ConfigurationProperties(prefix = "oss")
    @ToString
    public static class OssProperties {
        /**
         * 访问端点
         */
        private String endpoint;

        /**
         * 自定义域名
         */
        private String domain;

        /**
         * ACCESS_KEY
         */
        private String accessKey;

        /**
         * SECRET_KEY
         */
        private String secretKey;

        /**
         * 前缀
         */
        private String prefix;

        /**
         * 存储空间名
         */
        private String bucketName;

        /**
         * 存储区域
         */
        private String region;

        /**
         * 是否 https，Y 为是，N 为否
         */
        private String isHttps;

        /**
         * 桶权限类型，0 private，1 public，2 custom
         */
        private String accessPolicy;
    }
}
