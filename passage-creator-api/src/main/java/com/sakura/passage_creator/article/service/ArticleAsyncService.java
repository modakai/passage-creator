package com.sakura.passage_creator.article.service;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;
import com.sakura.passage_creator.article.agent.ContentGeneratorAgent;
import com.sakura.passage_creator.article.agent.ImageAnalyzerAgent;
import com.sakura.passage_creator.article.agent.OutlineGeneratorAgent;
import com.sakura.passage_creator.article.agent.TitleGeneratorAgent;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.image.service.ArticleImageGenerationService;
import com.sakura.passage_creator.article.image.service.ContentImageMerger;
import com.sakura.passage_creator.article.manager.SseEmitterManager;
import com.sakura.passage_creator.article.model.dto.SseMessage;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.model.enums.SseMessageTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.sakura.passage_creator.article.model.entity.table.ArticleTableDef.ARTICLE;

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
    private final OutlineGeneratorAgent outlineGeneratorAgent;
    private final ContentGeneratorAgent contentGeneratorAgent;
    private final ImageAnalyzerAgent imageAnalyzerAgent;
    private final ArticleImageGenerationService articleImageGenerationService;
    private final ContentImageMerger contentImageMerger;

    private final SseEmitterManager sseEmitterManager;

    @Async("articleExecutor")
    public void executePhase1(String taskId, String topic) {
        try {
            log.info("阶段1异步任务开始, taskId={}, topic={}",
                    taskId, topic);

            // 更新状态和阶段
            Db.tx(() -> articleService.updateStatus(ArticleStatusEnum.PROCESSING, taskId) &&
                    articleService.updatePhase(ArticlePhaseEnum.TITLE_GENERATING, taskId));
            sendPhaseChanged(taskId, ArticlePhaseEnum.TITLE_GENERATING);

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
        } catch (Exception exception) {
            handleFailure(taskId, "阶段1：生成标题失败", exception);
        }
    }

    /**
     * 生成大纲
     *
     * @param taskId          任务id
     * @param mainTitle       主标题
     * @param subTitle        副标题
     * @param userDescription 描述
     */
    @Async("articleExecutor")
    public void executePhase2(String taskId, String mainTitle, String subTitle, String userDescription) {
        try {
            sendPhaseChanged(taskId, ArticlePhaseEnum.OUTLINE_GENERATING);

            // 构建 智能体上下文
            ArticleState.TitleResult titleResult = new ArticleState.TitleResult();
            titleResult.setMainTitle(mainTitle);
            titleResult.setSubTitle(subTitle);
            ArticleState state = ArticleState.builder()
                    .taskId(taskId)
                    .title(titleResult)
                    .userDescription(userDescription)
                    .build();

            // 调用生成
            outlineGeneratorAgent.generatorOutline(state);

            // 更新状态
            ArticleState.OutlineResult outline = state.getOutline();
            Db.tx(() ->
                    articleService.saveOutline(outline, taskId)
                            // 更新阶段为等待用户编辑大纲
                            && articleService.updateStatus(ArticleStatusEnum.PENDING, taskId) &&
                            articleService.updatePhase(ArticlePhaseEnum.OUTLINE_EDITING, taskId)
            );

            // 推送大纲生成完成消息
            SseMessage<ArticleState.OutlineResult> message = SseMessage.of(SseMessageTypeEnum.OUTLINE_GENERATED, outline);
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
        } catch (Exception exception) {
            handleFailure(taskId, "阶段2：生成大纲失败", exception);
        }
    }

    /**
     * 生成正文
     *
     * @param taskId  任务id
     * @param outline 大纲
     */
    @Async("articleExecutor")
    public void executePhase3(String taskId, ArticleState.OutlineResult outline) {

        try {
            // 正文生成运行在线程池中，不能依赖请求线程里的 LoginUserContext。
            Db.tx(() -> articleService.updateStatus(ArticleStatusEnum.PROCESSING, taskId) &&
                    articleService.updatePhase(ArticlePhaseEnum.CONTENT_GENERATING, taskId));
            sendPhaseChanged(taskId, ArticlePhaseEnum.CONTENT_GENERATING);

            Article article = articleService.getOne(QueryWrapper.create()
                    .where(ARTICLE.TASK_ID.eq(taskId)));
            if (article == null) {
                throw new IllegalStateException("文章任务不存在");
            }

            ArticleState.TitleResult titleResult = new ArticleState.TitleResult();
            titleResult.setMainTitle(article.getMainTitle());
            titleResult.setSubTitle(article.getSubTitle());

            ArticleState articleState = ArticleState.builder()
                    .taskId(taskId)
                    .title(titleResult)
                    .outline(outline)
                    .build();

            contentGeneratorAgent.generatorContent(articleState);

            // 正文完成后进入配图分析，第一阶段不引入 workflow，但保留清晰阶段边界。
            Db.tx(() -> articleService.updatePhase(ArticlePhaseEnum.IMAGE_ANALYZING, taskId));
            sendPhaseChanged(taskId, ArticlePhaseEnum.IMAGE_ANALYZING);
            imageAnalyzerAgent.analyze(articleState);
            SseMessage<List<ArticleState.ImageRequirement>> imageAnalyzedMessage =
                    SseMessage.of(SseMessageTypeEnum.IMAGE_ANALYZED, articleState.getImageRequirements());
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(imageAnalyzedMessage));

            // 配图生成保持串行执行，便于先把 gpt-image-2 单链路跑通。
            Db.tx(() -> articleService.updatePhase(ArticlePhaseEnum.IMAGE_GENERATING, taskId));
            sendPhaseChanged(taskId, ArticlePhaseEnum.IMAGE_GENERATING);
            List<ArticleState.ImageResult> images = articleImageGenerationService.generateImages(
                    taskId,
                    articleState.getImageRequirements(),
                    image -> {
                        SseMessage<ArticleState.ImageResult> imageMessage =
                                SseMessage.of(SseMessageTypeEnum.IMAGE_COMPLETE, image);
                        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(imageMessage));
                    }
            );
            articleState.setImages(images);
            articleState.setCoverImage(resolveCoverImage(images));
            SseMessage<List<ArticleState.ImageResult>> imageGeneratedMessage =
                    SseMessage.of(SseMessageTypeEnum.IMAGE_GENERATED, images);
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(imageGeneratedMessage));

            // 图文合成只替换正文里的章节图占位符，封面图单独保存在 coverImage。
            Db.tx(() -> articleService.updatePhase(ArticlePhaseEnum.CONTENT_MERGING, taskId));
            sendPhaseChanged(taskId, ArticlePhaseEnum.CONTENT_MERGING);
            String fullContent = contentImageMerger.merge(articleState.getContent(), images);
            articleState.setFullContent(fullContent);
            SseMessage<String> mergeMessage = SseMessage.of(SseMessageTypeEnum.MERGE_COMPLETE, fullContent);
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(mergeMessage));

            if (!articleService.completeContentWithImages(taskId, articleState)) {
                throw new IllegalStateException("完整图文保存失败");
            }
            sendPhaseChanged(taskId, ArticlePhaseEnum.COMPLETED);

            SseMessage<String> message = SseMessage.of(SseMessageTypeEnum.ALL_COMPLETE, fullContent);
            sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
        } catch (Exception exception) {
            handleFailure(taskId, "阶段3：生成正文和配图失败", exception);
        }

    }

    /**
     * 获取封面图 URL。约定 position=1 为封面图。
     */
    private String resolveCoverImage(List<ArticleState.ImageResult> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(image -> image.getPosition() != null && image.getPosition() == 1)
                .map(ArticleState.ImageResult::getUrl)
                .findFirst()
                .orElse(null);
    }

    /**
     * 推送阶段变化消息，前端据此展示完整创作流程。
     */
    private void sendPhaseChanged(String taskId, ArticlePhaseEnum phase) {
        SseMessage<String> message = SseMessage.of(SseMessageTypeEnum.PHASE_CHANGED, phase.getValue());
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
    }

    /**
     * 统一处理异步阶段失败，保证数据库状态和前端 SSE 状态保持一致。
     *
     * @param taskId    任务 id
     * @param logPrefix 日志前缀
     * @param exception 异常
     */
    private void handleFailure(String taskId, String logPrefix, Exception exception) {
        log.error("{}, taskId={}", logPrefix, taskId, exception);
        String errorMessage = exception.getMessage();
        articleService.markFailed(taskId, errorMessage);
        SseMessage<String> message = SseMessage.of(SseMessageTypeEnum.ERROR, errorMessage);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
    }
}
