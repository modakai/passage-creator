package com.sakura.passage_creator.creation.workflow.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.sakura.passage_creator.creation.workflow.config.CreationWorkflowProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
public class RedisWorkflowCheckpointSaver implements BaseCheckpointSaver {

    private static final String KEY_PREFIX = "creation:workflow:checkpoint:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CreationWorkflowProperties properties;

    public RedisWorkflowCheckpointSaver(RedisTemplate<String, Object> redisTemplate,
                                        CreationWorkflowProperties properties) {
        this.redisTemplate = redisTemplate;
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
        return exists && get(RunnableConfig.builder().threadId(threadId).build()).isPresent();
    }

    private void saveCheckpoints(String threadId, LinkedList<Checkpoint> checkpoints) {
        List<CheckpointRecord> records = checkpoints.stream()
                .map(CheckpointRecord::fromCheckpoint)
                .toList();
        redisTemplate.opsForValue().set(key(threadId), records, ttl());
    }

    @SuppressWarnings("unchecked")
    private LinkedList<Checkpoint> loadCheckpoints(String threadId) {
        Object value = redisTemplate.opsForValue().get(key(threadId));
        if (!(value instanceof List<?> records)) {
            return new LinkedList<>();
        }
        LinkedList<Checkpoint> checkpoints = new LinkedList<>();
        for (Object record : records) {
            if (record instanceof CheckpointRecord checkpointRecord) {
                checkpoints.add(checkpointRecord.toCheckpoint());
            } else if (record instanceof Map<?, ?> map) {
                // Jackson 反序列化为 Map 时也能恢复，避免 RedisTemplate 类型信息差异导致 checkpoint 丢失。
                checkpoints.add(CheckpointRecord.fromMap((Map<String, Object>) map).toCheckpoint());
            }
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

        static CheckpointRecord fromMap(Map<String, Object> map) {
            CheckpointRecord record = new CheckpointRecord();
            record.setId((String) map.get("id"));
            record.setState((Map<String, Object>) map.get("state"));
            record.setNodeId((String) map.get("nodeId"));
            record.setNextNodeId((String) map.get("nextNodeId"));
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
