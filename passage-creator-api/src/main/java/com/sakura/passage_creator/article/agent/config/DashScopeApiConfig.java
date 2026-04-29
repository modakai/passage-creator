package com.sakura.passage_creator.article.agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author sakura
 * @create 2026-04
 */
@Configuration
public class DashScopeApiConfig {

    @Value("${spring.ai.dashscope.api-key}")
    @Setter
    private String apiKey;

    @Bean
    public DashScopeApi dashScopeApi() {
        // 创建 DashScope API 实例
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
    }
}
