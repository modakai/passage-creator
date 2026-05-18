package com.sakura.passage_creator.article.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文章生成 SSE 连接管理器。
 *
 * @author sakura
 * @create 2026-04
 */
@Component
@Slf4j
public class SseEmitterManager {

    /**
     * SSE 连接超时时间，30 分钟足够覆盖当前同步 Agent MVP 的生成过程。
     */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * 浏览器断线后的重连间隔。
     */
    private static final long SSE_RECONNECT_TIME_MS = 3000L;

    /**
     * 按文章任务 id 保存当前活跃的 SSE 连接。
     */
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建 SSE 连接，同一个 taskId 重复连接时覆盖旧连接。
     *
     * @param taskId 文章任务 id
     * @return SSE 连接对象
     */
    public SseEmitter createEmitter(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            throw new IllegalArgumentException("taskId 不能为空");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时, taskId={}", taskId);
            emitterMap.remove(taskId, emitter);
        });
        emitter.onCompletion(() -> {
            log.info("SSE 连接完成, taskId={}", taskId);
            emitterMap.remove(taskId, emitter);
        });
        emitter.onError(error -> {
            log.error("SSE 连接异常, taskId={}", taskId, error);
            emitterMap.remove(taskId, emitter);
        });

        SseEmitter oldEmitter = emitterMap.put(taskId, emitter);
        if (oldEmitter != null) {
            // 同一个任务只保留最新连接，避免旧页面继续占用内存。
            oldEmitter.complete();
        }
        log.info("SSE 连接已创建, taskId={}", taskId);
        return emitter;
    }

    /**
     * 向指定任务发送 SSE 消息。
     *
     * @param taskId  文章任务 id
     * @param message 消息内容，调用方负责传入前端可解析的字符串
     */
    public void send(String taskId, String message) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE 连接不存在, taskId={}", taskId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .data(message)
                    .reconnectTime(SSE_RECONNECT_TIME_MS));
            log.debug("SSE 消息发送成功, taskId={}, message={}", taskId, message);
        } catch (IOException e) {
            log.error("SSE 消息发送失败, taskId={}", taskId, e);
            emitterMap.remove(taskId);
        }
    }

    /**
     * 完成并移除指定任务的 SSE 连接。
     *
     * @param taskId 文章任务 id
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            log.warn("SSE 连接不存在, taskId={}", taskId);
            return;
        }

        try {
            emitter.complete();
            log.info("SSE 连接已完成, taskId={}", taskId);
        } catch (Exception e) {
            log.error("SSE 连接关闭失败, taskId={}", taskId, e);
        } finally {
            emitterMap.remove(taskId);
        }
    }

    /**
     * 判断指定任务是否存在活跃 SSE 连接。
     *
     * @param taskId 文章任务 id
     * @return 是否存在连接
     */
    public boolean exists(String taskId) {
        return emitterMap.containsKey(taskId);
    }

    /**
     * Spring 容器关闭时主动完成所有文章 SSE，兼容仍在使用的旧文章生成链路。
     */
    @PreDestroy
    public void destroy() {
        if (emitterMap.isEmpty()) {
            return;
        }
        log.info("文章 SSE 管理器销毁，准备关闭活跃连接数量={}", emitterMap.size());
        emitterMap.forEach((taskId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 停服时客户端可能已经断开，忽略二次关闭异常。
            } finally {
                emitterMap.remove(taskId, emitter);
            }
        });
    }
}
