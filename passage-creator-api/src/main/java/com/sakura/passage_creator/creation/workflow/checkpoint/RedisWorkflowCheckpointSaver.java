package com.sakura.passage_creator.creation.workflow.checkpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.sakura.passage_creator.creation.workflow.config.CreationWorkflowProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis checkpoint saver，让 Spring AI Alibaba StateGraph 的中断点可跨服务重启恢复。
 */
@Component
@Slf4j
public class RedisWorkflowCheckpointSaver implements BaseCheckpointSaver {

    private static final String KEY_PREFIX = "creation:workflow:checkpoint:";
    private static final TypeReference<List<CheckpointRecord>> CHECKPOINT_RECORD_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CreationWorkflowProperties properties;

    public RedisWorkflowCheckpointSaver(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            CreationWorkflowProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        return loadCheckpoints(threadId(config));
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        LinkedList<Checkpoint> checkpoints = loadCheckpoints(threadId(config));
        Optional<String> checkpointId = config.checkPointId();
        if (checkpointId.isEmpty()) {
            return getLast(checkpoints, config);
        }
        return checkpoints.stream()
                .filter(checkpoint -> checkpointId.get().equals(checkpoint.getId()))
                .findFirst();
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) {
        String threadId = threadId(config);
        LinkedList<Checkpoint> checkpoints = loadCheckpoints(threadId);
        Optional<String> checkpointId = config.checkPointId();
        if (checkpointId.isPresent()) {
            // updateState 会带 checkpointId，需原位替换，避免链表头顺序被打乱。
            for (int index = 0; index < checkpoints.size(); index++) {
                if (checkpointId.get().equals(checkpoints.get(index).getId())) {
                    checkpoints.set(index, checkpoint);
                    saveCheckpoints(threadId, checkpoints);
                    return config;
                }
            }
        }

        // 新执行产生的 checkpoint 放在链表头，保持和 MemorySaver 的恢复语义一致。
        checkpoints.addFirst(checkpoint);
        saveCheckpoints(threadId, checkpoints);
        return RunnableConfig.builder(config)
                .checkPointId(checkpoint.getId())
                .build();
    }

    @Override
    public Tag release(RunnableConfig config) {
        String threadId = threadId(config);
        LinkedList<Checkpoint> checkpoints = loadCheckpoints(threadId);
        redisTemplate.delete(key(threadId));
        return new Tag(threadId, checkpoints);
    }

    /**
     * 判断指定 threadId 是否还有可恢复 checkpoint。
     */
    public boolean exists(String threadId) {
        Boolean exists = redisTemplate.hasKey(key(threadId));
        return Boolean.TRUE.equals(exists) && get(RunnableConfig.builder().threadId(threadId).build()).isPresent();
    }

    private void saveCheckpoints(String threadId, LinkedList<Checkpoint> checkpoints) {
        List<CheckpointRecord> records = checkpoints.stream()
                .map(CheckpointRecord::fromCheckpoint)
                .toList();
        try {
            // 使用专用 JSON 字符串保存 checkpoint，避免全局 RedisTemplate 的 default typing 影响 Graph 恢复。
            redisTemplate.opsForValue().set(key(threadId), objectMapper.writeValueAsString(records), ttl());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 workflow checkpoint 失败", e);
        }
    }

    private LinkedList<Checkpoint> loadCheckpoints(String threadId) {
        String value = redisTemplate.opsForValue().get(key(threadId));
        if (value == null || value.isBlank()) {
            return new LinkedList<>();
        }
        List<CheckpointRecord> records;
        try {
            records = objectMapper.readValue(value, CHECKPOINT_RECORD_LIST_TYPE);
        } catch (JsonProcessingException e) {
            // 旧版本可能写入了不兼容的 Redis 序列化结构；删除坏 checkpoint，让业务层走过期恢复分支。
            log.warn("workflow checkpoint 反序列化失败，已删除不兼容 checkpoint, threadId={}", threadId, e);
            redisTemplate.delete(key(threadId));
            return new LinkedList<>();
        }
        LinkedList<Checkpoint> checkpoints = new LinkedList<>();
        for (CheckpointRecord record : records) {
            checkpoints.add(record.toCheckpoint());
        }
        return checkpoints;
    }

    private Duration ttl() {
        Duration ttl = properties.getCheckpointTtl();
        return ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofDays(7) : ttl;
    }

    private String threadId(RunnableConfig config) {
        return config.threadId().orElse(THREAD_ID_DEFAULT);
    }

    private String key(String threadId) {
        return KEY_PREFIX + threadId;
    }

    /**
     * Redis 中保存稳定 DTO，不直接依赖 Checkpoint 私有构造和序列化实现。
     */
    @Data
    @NoArgsConstructor
    public static class CheckpointRecord {
        private String id;
        private Map<String, Object> state;
        private String nodeId;
        private String nextNodeId;

        static CheckpointRecord fromCheckpoint(Checkpoint checkpoint) {
            CheckpointRecord record = new CheckpointRecord();
            record.setId(checkpoint.getId());
            record.setState(checkpoint.getState());
            record.setNodeId(checkpoint.getNodeId());
            record.setNextNodeId(checkpoint.getNextNodeId());
            return record;
        }
        Checkpoint toCheckpoint() {
            return Checkpoint.builder()
                    .id(id)
                    .state(state)
                    .nodeId(nodeId)
                    .nextNodeId(nextNodeId)
                    .build();
        }
    }
}
