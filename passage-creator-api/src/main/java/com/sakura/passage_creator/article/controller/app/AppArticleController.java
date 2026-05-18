package com.sakura.passage_creator.article.controller.app;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.creation.workflow.image.WorkflowRemoteImageDownloader;
import com.sakura.passage_creator.creation.workflow.service.WorkflowSseEmitterManager;
import com.sakura.passage_creator.article.model.dto.ArticleConfirmOutlineRequest;
import com.sakura.passage_creator.article.model.dto.ArticleConfirmTitleRequest;
import com.sakura.passage_creator.article.model.dto.ArticleCreateRequest;
import com.sakura.passage_creator.article.model.dto.ArticleQueryRequest;
import com.sakura.passage_creator.article.model.dto.SseMessage;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.SseMessageTypeEnum;
import com.sakura.passage_creator.article.model.vo.ArticleVO;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.article.workflow.ArticleWorkflowFacade;
import com.sakura.passage_creator.shared.common.BaseResponse;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.common.ResultUtils;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
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

    private final ArticleWorkflowFacade articleWorkflowFacade;

    private final ArticleService articleService;

    private final WorkflowSseEmitterManager sseEmitterManager;

    private final WorkflowRemoteImageDownloader remoteImageDownloader;

    /**
     * 创建文章任务，启动标题生成异步任务，返回 taskId
     */
    @PostMapping("/create")
    public BaseResponse<String> createArticleTask(@RequestBody @Valid ArticleCreateRequest request) {

        LoginUserInfo loginUser = LoginUserContext.getLoginUser();

        // 创建文章 workflow，标题生成由 workflow 异步推进。
        String taskId = articleWorkflowFacade.createArticleWorkflow(request, loginUser);

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
     * 下载本人文章已生成的图片，后端代理 OSS 请求，避免浏览器被 OSS CORS 限制。
     */
    @GetMapping("/image/download")
    public ResponseEntity<byte[]> downloadArticleImage(@RequestParam String taskId, @RequestParam String imageUrl) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(imageUrl), ErrorCode.PARAMS_ERROR, "图片地址不能为空");
        Article article = articleService.getOwnedArticleByTaskId(taskId, LoginUserContext.getLoginUser());
        assertImageBelongsToArticle(article, imageUrl);

        WorkflowImageData imageData = remoteImageDownloader.download(imageUrl);
        String fileName = "article-%s-image%s".formatted(taskId, imageData.getExtension());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(imageData.getMimeType()))
                .body(imageData.getBytes());
    }


    /**
     * 确认标题
     */
    @PostMapping("/confirm-title")
    public BaseResponse<Boolean> confirmTitle(@Valid @RequestBody ArticleConfirmTitleRequest request) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        return ResultUtils.success(articleWorkflowFacade.confirmTitle(request, loginUser));
    }

    /**
     * 确认标题
     */
    @PostMapping("/confirm-outline")
    public BaseResponse<Boolean> confirmOutline(@Valid @RequestBody ArticleConfirmOutlineRequest request) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        return ResultUtils.success(articleWorkflowFacade.confirmOutline(request, loginUser));
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
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        // 先做归属校验，再注册 SSE；注册后重新读取快照，避免生成完成事件夹在查询和建连之间被丢失。
        articleService.getOwnedArticleByTaskId(taskId, loginUser);
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        Article latestArticle = articleService.getOwnedArticleByTaskId(taskId, loginUser);
        ArticleVO articleVO = articleService.getArticleVO(latestArticle);
        SseMessage<ArticleVO> message = SseMessage.of(SseMessageTypeEnum.PROGRESS, articleVO);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
        // 恢复 Human-in-the-Loop 等待态，避免刷新页面后只看到文章快照而缺少确认表单。
        sendPendingHumanTaskIfPresent(taskId);
        return emitter;
    }

    /**
     * SSE 重连时补发当前等待中的人工任务。
     */
    private void sendPendingHumanTaskIfPresent(String taskId) {
        articleWorkflowFacade.publishPendingHumanTaskIfPresent(taskId);
    }

    /**
     * 下载接口只允许访问当前文章已经保存过的图片 URL，避免变成任意远程地址代理。
     */
    private void assertImageBelongsToArticle(Article article, String imageUrl) {
        if (imageUrl.equals(article.getCoverImage())) {
            return;
        }
        if (StringUtils.isBlank(article.getImages())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片不属于当前文章");
        }
        List<ArticleState.ImageResult> images = JSONUtil.toList(article.getImages(), ArticleState.ImageResult.class);
        boolean matched = images.stream()
                .anyMatch(image -> image != null && imageUrl.equals(image.getUrl()));
        if (!matched) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片不属于当前文章");
        }
    }
}
