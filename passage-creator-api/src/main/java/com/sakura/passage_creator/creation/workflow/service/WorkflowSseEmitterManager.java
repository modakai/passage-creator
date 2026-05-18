package com.sakura.passage_creator.creation.workflow.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 workflow SSE 连接管理器，供 article、rednote 等创作任务复用。
 */
@Component
@Slf4j
public class WorkflowSseEmitterManager {

    /**
     * SSE 连接超时时间，30 分钟覆盖常规 AI 生成流程。
     */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * 浏览器断线后的重连间隔。
     */
    private static final long SSE_RECONNECT_TIME_MS = 3000L;

    /**
     * 按 workflow taskId 保存当前活跃 SSE 连接。
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建 SSE 连接，同一个 taskId 重复连接时覆盖旧连接。
     */
    public SseEmitter createEmitter(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(() -> {
            log.warn("workflow SSE 连接超时, taskId={}", taskId);
            emitterMap.remove(taskId, emitter);
        });
        emitter.onCompletion(() -> {
            log.debug("workflow SSE 连接完成, taskId={}", taskId);
            emitterMap.remove(taskId, emitter);
        });
        emitter.onError(error -> {
            log.warn("workflow SSE 连接断开, taskId={}, reason={}", taskId, error.getMessage());
            emitterMap.remove(taskId, emitter);
        });

        SseEmitter oldEmitter = emitterMap.put(taskId, emitter);
        if (oldEmitter != null) {
            // 同一个任务只保留最新连接，避免旧页面继续占用内存。
            safeComplete(oldEmitter);
        }
        log.info("workflow SSE 连接已创建, taskId={}", taskId);
        return emitter;
    }

    /**
     * 向指定任务发送 SSE 消息，调用方负责传入前端可解析的 JSON 字符串。
     */
    public void send(String taskId, String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.debug("workflow SSE 连接不存在, taskId={}", taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(message)
                    .reconnectTime(SSE_RECONNECT_TIME_MS));
            log.debug("workflow SSE 消息发送成功, taskId={}, message={}", taskId, message);
        } catch (Exception e) {
            emitterMap.remove(taskId, emitter);
            safeComplete(emitter);
            if (isClientDisconnected(e)) {
                log.warn("workflow SSE 客户端已断开, taskId={}, reason={}", taskId, e.getMessage());
                return;
            }
            log.warn("workflow SSE 消息发送失败, taskId={}, reason={}", taskId, e.getMessage());
        }
    }

    /**
     * 完成并移除指定任务的 SSE 连接。
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("workflow SSE 连接不存在, taskId={}", taskId);
            return;
        }

        try {
            emitter.complete();
            log.debug("workflow SSE 连接已完成, taskId={}", taskId);
        } catch (Exception e) {
            log.debug("workflow SSE 连接关闭失败, taskId={}, reason={}", taskId, e.getMessage());
        } finally {
            emitterMap.remove(taskId);
        }
    }

    /**
     * 判断指定任务是否存在活跃 SSE 连接。
     */
    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }

    /**
     * 安静关闭 SSE，断线场景下不应污染业务错误日志。
     */
    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // 客户端断开时 complete 可能再次触发容器异常，忽略即可。
        }
    }

    /**
     * 判断是否为浏览器刷新、切页、AbortController 触发的正常断线。
     */
    private boolean isClientDisconnected(Exception e) {
        return e instanceof IOException
                || e instanceof AsyncRequestNotUsableException
                || e.getClass().getName().contains("ClientAbort")
                || StringUtils.containsIgnoreCase(e.getMessage(), "response errors")
                || StringUtils.containsIgnoreCase(e.getMessage(), "broken pipe");
    }
}
