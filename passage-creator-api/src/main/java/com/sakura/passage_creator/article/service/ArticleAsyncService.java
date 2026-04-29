package com.sakura.passage_creator.article.service;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.row.Db;
import com.sakura.passage_creator.article.agent.TitleGeneratorAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.manager.SseEmitterManager;
import com.sakura.passage_creator.article.model.dto.SseMessage;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.model.enums.SseMessageTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 创建文章异步
 *
 * @author sakura
 * @create 2026-04
 */
@Service
@Slf4j

@RequiredArgsConstructor
public class ArticleAsyncService {

    private final ArticleService articleService;

    private final TitleGeneratorAgent titleGeneratorAgent;

    private final SseEmitterManager sseEmitterManager;

    @Async("articleExecutor")
    public void executePhase1(String taskId, String topic) {
        log.info("阶段1异步任务开始, taskId={}, topic={}",
                taskId, topic);

        // 更新状态和阶段
        Db.tx(() -> articleService.updateStatus(ArticleStatusEnum.PROCESSING, taskId) &&
                articleService.updatePhase(ArticlePhaseEnum.TITLE_GENERATING, taskId));

        // 创建状态对象
        ArticleState articleState = new ArticleState();
        articleState.setTaskId(taskId);
        articleState.setTopic(topic);

        // 执行阶段1：生成标题方案（根据配置选择执行方式）
        titleGeneratorAgent.generatorTitle(articleState);

        // 保存标题方案到数据库
        List<ArticleState.TitleOption> titleOptions = articleState.getTitleOptions();
        Db.tx(() ->
                articleService.saveTitleOptions(titleOptions, taskId)
                        // 更新阶段为等待选择标题
                        && articleService.updateStatus(ArticleStatusEnum.PENDING, taskId) &&
                        articleService.updatePhase(ArticlePhaseEnum.TITLE_SELECTING, taskId)
        );

        // 推送标题方案生成完成消息
        SseMessage<List<ArticleState.TitleOption>> message = SseMessage.of(SseMessageTypeEnum.TITLES_GENERATED, titleOptions);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
    }
}
