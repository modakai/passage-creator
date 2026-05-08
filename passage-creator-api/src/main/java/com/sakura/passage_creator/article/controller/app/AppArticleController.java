package com.sakura.passage_creator.article.controller.app;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.article.manager.SseEmitterManager;
import com.sakura.passage_creator.article.model.dto.ArticleConfirmOutlineRequest;
import com.sakura.passage_creator.article.model.dto.ArticleConfirmTitleRequest;
import com.sakura.passage_creator.article.model.dto.ArticleCreateRequest;
import com.sakura.passage_creator.article.model.dto.ArticleQueryRequest;
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
import io.github.linpeilie.Converter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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

    private final Converter converter;

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
     * 用户端分页查看本人文章创建记录。
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ArticleVO>> listMyArticleByPage(@Valid @RequestBody ArticleQueryRequest queryRequest) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        long current = queryRequest.getPage();
        long pageSize = queryRequest.getPageSize();
        // 用户端记录页始终查询本人数据，管理员在前台也只能看到自己的创作记录。
        queryRequest.setUserId(loginUser.userId());
        Page<Article> page = articleService.page(new Page<>(current, pageSize),
                articleService.getQueryWrapper(queryRequest, loginUser));
        List<ArticleVO> articleVOList = articleService.getArticleVO(page.getRecords());
        Page<ArticleVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(articleVOList);
        return ResultUtils.success(voPage);
    }

    /**
     * 用户端根据 id 查看本人文章创建记录详情。
     */
    @GetMapping("/get")
    public BaseResponse<ArticleVO> getMyArticleById(@RequestParam @Positive(message = "文章 id 必须大于 0") long id) {
        Article article = articleService.getOwnedArticle(id, LoginUserContext.getLoginUser());
        return ResultUtils.success(articleService.getArticleVO(article));
    }


    /**
     * 确认标题
     */
    @PostMapping("/confirm-title")
    public BaseResponse<Boolean> confirmTitle(@Valid @RequestBody ArticleConfirmTitleRequest request) {
        // 确认标题入库
        Article article = converter.convert(request, Article.class);
        // DTO 使用 selected* 表达前端选择结果，这里显式映射到文章实体标题字段。
        article.setMainTitle(request.getSelectedMainTitle());
        article.setSubTitle(request.getSelectedSubTitle());
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        Long userId = loginUser.userId();
        article.setUserId(userId);
        boolean flag = articleService.confirmTitle(article);
        if (!flag) return ResultUtils.success(false);

        // 异步生成大纲
        articleAsyncService.executePhase2(article.getTaskId(), article.getMainTitle(),
                article.getSubTitle(), article.getUserDescription());

        return ResultUtils.success(true);
    }

    /**
     * 确认标题
     */
    @PostMapping("/confirm-outline")
    public BaseResponse<Boolean> confirmOutline(@Valid @RequestBody ArticleConfirmOutlineRequest request) {

        boolean flag = articleService.confirmOutline(request.getTaskId(), request.getOutline());

        if (!flag) return ResultUtils.success(flag);

        // 生成正文
        articleAsyncService.executePhase3(request.getTaskId(), request.getOutline());

        return ResultUtils.success(true);
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
        Article article = articleService.getOwnedArticleByTaskId(taskId, LoginUserContext.getLoginUser());
        // SSE 连接由管理器按 taskId 保存，后续异步阶段通过 taskId 推送消息。
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        ArticleVO articleVO = articleService.getArticleVO(article);
        SseMessage<ArticleVO> message = SseMessage.of(SseMessageTypeEnum.PROGRESS, articleVO);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
        return emitter;
    }
}
