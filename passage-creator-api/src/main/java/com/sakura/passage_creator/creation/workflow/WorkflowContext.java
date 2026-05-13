package com.sakura.passage_creator.creation.workflow;

import cn.hutool.json.JSONUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Workflow 上下文，负责保存节点间传递的结构化状态快照。
 */
@Getter
public class WorkflowContext implements Serializable {

    /**
     * 有序保存上下文字段，便于日志快照稳定输出。
     */
    private final Map<String, Object> values;

    public WorkflowContext() {
        this.values = new LinkedHashMap<>();
    }

    private WorkflowContext(Map<String, Object> values) {
        this.values = new LinkedHashMap<>(values);
    }

    /**
     * 基于 Map 创建上下文。
     */
    public static WorkflowContext fromMap(Map<String, ?> values) {
        return new WorkflowContext(values == null ? Map.of() : new LinkedHashMap<>(values));
    }

    /**
     * 基于 JSON 快照恢复上下文。
     */
    public static WorkflowContext fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new WorkflowContext();
        }
        return new WorkflowContext(JSONUtil.parseObj(json));
    }

    /**
     * 合并节点输出。
     */
    public void putAll(Map<String, ?> updates) {
        if (updates != null) {
            values.putAll(updates);
        }
    }

    /**
     * 写入单个上下文字段。
     */
    public void put(String key, Object value) {
        values.put(key, value);
    }

    /**
     * 获取字符串字段。
     */
    public String getString(String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 获取 Long 字段。
     */
    public Long getLong(String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    /**
     * 获取 Boolean 字段。
     */
    public Boolean getBoolean(String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(value.toString());
    }

    /**
     * 将字段转换为目标 Bean。
     */
    public <T> T getBean(String key, Class<T> targetType) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        return JSONUtil.toBean(JSONUtil.toJsonStr(value), targetType);
    }

    /**
     * 序列化为 JSON 快照。
     */
    public String toJson() {
        return JSONUtil.toJsonStr(values);
    }
}
