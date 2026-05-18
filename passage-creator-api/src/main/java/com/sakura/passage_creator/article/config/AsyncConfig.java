package com.sakura.passage_creator.article.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 文章生成异步线程池
     */
    @Bean(name = "articleExecutor")
    public Executor articleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名称前缀
        executor.setThreadNamePrefix("article-async-");

        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        return executor;
    }

    /**
     * 小红书 workflow 异步线程池，和文章生成隔离，避免两类 AI 任务互相挤占队列。
     */
    @Bean(name = "rednoteExecutor")
    public Executor rednoteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 小红书任务包含搜索、文案、提示词和图片生成，核心线程数保持和文章池一致。
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(100);

        // 线程名称前缀，便于日志中区分 rednote workflow。
        executor.setThreadNamePrefix("rednote-async-");

        // 拒绝策略：由调用线程处理，避免任务被直接丢弃。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        return executor;
    }
}
