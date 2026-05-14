package com.sakura.passage_creator.creation.workflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 创作 workflow 配置，集中管理 checkpoint 和人工任务的生命周期。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "creation.workflow")
public class CreationWorkflowProperties {

    /**
     * checkpoint 与人工确认任务的有效期；超过该时间必须重新生成，不能继续恢复旧 Graph。
     */
    private Duration checkpointTtl = Duration.ofDays(7);
}
