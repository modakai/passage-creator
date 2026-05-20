package com.sakura.passage_creator.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakura.passage_creator.prompt.model.entity.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ACTIVE Prompt Redis 缓存，数据库仍然是模板发布的唯一权威来源。
 */
@Slf4j
@Component
public class PromptActiveTemplateCache {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public PromptActiveTemplateCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper.findAndRegisterModules();
    }

    /**
     * 从 Redis 读取 ACTIVE 模板快照，读取失败时交给数据库兜底。
     */
    public PromptTemplate get(String cacheKey) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, Snapshot.class).toTemplate();
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("读取 ACTIVE Prompt Redis 缓存失败, cacheKey={}", cacheKey, e);
            return null;
        }
    }

    /**
     * 写入 ACTIVE 模板快照，只缓存运行时渲染和日志追踪需要的字段。
     */
    public void put(String cacheKey, PromptTemplate template) {
        if (template == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(Snapshot.from(template)));
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("写入 ACTIVE Prompt Redis 缓存失败, cacheKey={}", cacheKey, e);
        }
    }

    /**
     * 删除 ACTIVE 模板快照，发布、归档、删除和手动刷新时统一调用。
     */
    public void evict(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (RuntimeException e) {
            log.warn("删除 ACTIVE Prompt Redis 缓存失败, cacheKey={}", cacheKey, e);
        }
    }

    /**
     * Redis 缓存 DTO，避免直接序列化 MyBatis 实体导致字段或代理变化影响反序列化。
     */
    public record Snapshot(
            Long id,
            String templateKey,
            String version,
            String environment,
            String content,
            String variablesSchema,
            LocalDateTime publishedAt
    ) {

        public static Snapshot from(PromptTemplate template) {
            return new Snapshot(
                    template.getId(),
                    template.getTemplateKey(),
                    template.getVersion(),
                    template.getEnvironment(),
                    template.getContent(),
                    template.getVariablesSchema(),
                    template.getPublishedAt()
            );
        }

        /**
         * 将缓存快照还原为 PromptTemplate，供现有渲染链路复用。
         */
        public PromptTemplate toTemplate() {
            PromptTemplate template = new PromptTemplate();
            template.setId(id);
            template.setTemplateKey(templateKey);
            template.setVersion(version);
            template.setEnvironment(environment);
            template.setContent(content);
            template.setVariablesSchema(variablesSchema);
            template.setPublishedAt(publishedAt);
            return template;
        }
    }
}
