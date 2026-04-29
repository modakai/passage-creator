package com.sakura.passage_creator.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;

/**
 * 数据库连接池启动预热组件。
 *
 * <p>该组件默认关闭，仅在显式开启时异步尝试获取一次连接，避免数据库短暂不可用时阻塞应用启动。
 */
@Slf4j
@Order(0)
@Component
@ConditionalOnProperty(prefix = "datasource.warmup", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataSourceWarmup implements ApplicationRunner {

    private final DataSource dataSource;

    /**
     * 应用启动完成后异步触发一次连接获取，避免把数据库预热变成启动前置条件。
     */
    @Override
    public void run(ApplicationArguments args) {
        // 预热放到后台线程执行，保证应用上下文先完成启动。
        CompletableFuture.runAsync(() -> {
            try (Connection ignored = dataSource.getConnection()) {
                log.info("数据库连接池预热完成");
            } catch (Exception ex) {
                log.warn("数据库连接池预热失败，已忽略，不影响应用启动", ex);
            }
        });
    }
}
