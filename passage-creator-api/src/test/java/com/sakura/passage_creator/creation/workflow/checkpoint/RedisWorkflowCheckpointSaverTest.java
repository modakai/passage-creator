package com.sakura.passage_creator.creation.workflow.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sakura.passage_creator.creation.workflow.config.CreationWorkflowProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis checkpoint saver 测试，覆盖 TTL 写入和 checkpoint 恢复。
 */
class RedisWorkflowCheckpointSaverTest {

    @Test
    void shouldPersistCheckpointWithConfiguredTtlAndRestoreIt() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        CreationWorkflowProperties properties = new CreationWorkflowProperties();
        properties.setCheckpointTtl(Duration.ofDays(7));
        RedisWorkflowCheckpointSaver saver = new RedisWorkflowCheckpointSaver(redisTemplate, new ObjectMapper(), properties);
        String redisKey = "creation:workflow:checkpoint:task-1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        Checkpoint checkpoint = Checkpoint.builder()
                .id("checkpoint-1")
                .state(Map.of("selectedMainTitle", "标题 A"))
                .nodeId("TITLE_GENERATING")
                .nextNodeId("TITLE_CONFIRM")
                .build();
        RunnableConfig config = RunnableConfig.builder().threadId("task-1").build();
        RunnableConfig savedConfig = saver.put(config, checkpoint);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(redisKey), valueCaptor.capture(), eq(Duration.ofDays(7)));
        when(valueOperations.get(redisKey)).thenReturn(valueCaptor.getValue());

        assertThat(savedConfig.checkPointId()).contains("checkpoint-1");
        assertThat(saver.get(RunnableConfig.builder().threadId("task-1").build()))
                .get()
                .extracting(Checkpoint::getId)
                .isEqualTo("checkpoint-1");
    }

    @Test
    void shouldReadPlainJsonArrayWithoutRedisTemplateTypeMetadata() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        CreationWorkflowProperties properties = new CreationWorkflowProperties();
        RedisWorkflowCheckpointSaver saver = new RedisWorkflowCheckpointSaver(redisTemplate, new ObjectMapper(), properties);
        String redisKey = "creation:workflow:checkpoint:task-1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn("""
                [{"id":"checkpoint-1","state":{"selectedMainTitle":"标题 A"},"nodeId":"TITLE_GENERATING","nextNodeId":"TITLE_CONFIRM"}]
                """);

        assertThat(saver.get(RunnableConfig.builder().threadId("task-1").build()))
                .get()
                .extracting(checkpoint -> checkpoint.getState().get("selectedMainTitle"))
                .isEqualTo("标题 A");
    }
}
