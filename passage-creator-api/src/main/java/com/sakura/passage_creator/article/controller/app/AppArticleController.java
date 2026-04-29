package com.sakura.passage_creator.article.controller.app;

import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.article.manager.SseEmitterManager;
import com.sakura.passage_creator.article.model.dto.ArticleCreateRequest;
import com.sakura.passage_creator.article.model.dto.SseMessage;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.SseMessageTypeEnum;
import com.sakura.passage_creator.article.model.vo.ArticleVO;
import com.sakura.passage_creator.article.service.ArticleAsyncService;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户端 文章接口
 *
 * @author sakura
 * @create 2026-04
 */
@RestController
@RequestMapping("/app/article")
@RequiredArgsConstructor
public class AppArticleController {

    private final ArticleAsyncService articleAsyncService;

    private final ArticleService articleService;

    private final SseEmitterManager sseEmitterManager;

    /**
     * 创建文章任务，启动标题生成异步任务，返回 taskId
     */
    @PostMapping("/create")
    public BaseResponse<String> createArticleTask(@RequestBody @Valid ArticleCreateRequest request) {

        LoginUserInfo loginUser = LoginUserContext.getLoginUser();

        // 创建文章
        String taskId = articleService.createArticle(request.getTopic(), loginUser);

        // 异步构建
        articleAsyncService.executePhase1(taskId, request.getTopic());

        return ResultUtils.success(taskId);
    }

    /**
     * 建立文章生成进度 SSE 连接。
     */
    @GetMapping(value = "/sse/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(@PathVariable String taskId) {
        return createProgressEmitter(taskId);
    }

    /**
     * 建立文章生成进度 SSE 连接，接口名称和前端文章创作流程保持一致。
     */
    @GetMapping(value = "/progress/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getProgress(@PathVariable String taskId) {
        return createProgressEmitter(taskId);
    }

    /**
     * 创建 SSE 连接并立即推送当前任务快照，避免前端错过早期生成事件。
     */
    private SseEmitter createProgressEmitter(String taskId) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        Article article = articleService.getAccessibleArticleByTaskId(taskId, LoginUserContext.getLoginUser());
        // SSE 连接由管理器按 taskId 保存，后续异步阶段通过 taskId 推送消息。
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        ArticleVO articleVO = articleService.getArticleVO(article);
        SseMessage<ArticleVO> message = SseMessage.of(SseMessageTypeEnum.PROGRESS, articleVO);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
        return emitter;
    }
}
