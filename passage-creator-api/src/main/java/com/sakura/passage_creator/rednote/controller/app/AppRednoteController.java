package com.sakura.passage_creator.rednote.controller.app;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.creation.workflow.WorkflowSseMessage;
import com.sakura.passage_creator.creation.workflow.image.WorkflowImageData;
import com.sakura.passage_creator.creation.workflow.image.WorkflowRemoteImageDownloader;
import com.sakura.passage_creator.creation.workflow.service.WorkflowSseEmitterManager;
import com.sakura.passage_creator.rednote.model.dto.RednoteCreateRequest;
import com.sakura.passage_creator.rednote.model.dto.RednoteQueryRequest;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.model.vo.RednoteNoteVO;
import com.sakura.passage_creator.rednote.service.RednoteNotePersistenceService;
import com.sakura.passage_creator.rednote.workflow.RednoteWorkflowFacade;
import com.sakura.passage_creator.rednote.workflow.state.RednoteWorkflowState;
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
 * 用户端小红书爆款笔记创作接口。
 */
@RestController
@RequestMapping("/app/rednote")
@RequiredArgsConstructor
public class AppRednoteController {

    private final RednoteWorkflowFacade rednoteWorkflowFacade;

    private final RednoteNotePersistenceService rednoteNoteService;

    private final WorkflowSseEmitterManager sseEmitterManager;

    private final WorkflowRemoteImageDownloader remoteImageDownloader;

    /**
     * 创建小红书创作任务，接收单字段 content 并异步启动全自动 workflow。
     */
    @PostMapping("/create")
    public BaseResponse<String> createRednoteTask(@RequestBody @Valid RednoteCreateRequest request) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        String taskId = rednoteWorkflowFacade.createRednoteWorkflow(request, loginUser);
        return ResultUtils.success(taskId);
    }

    /**
     * 用户端分页查看本人小红书创作记录。
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<RednoteNoteVO>> listMyRednoteByPage(@Valid @RequestBody RednoteQueryRequest queryRequest) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        long current = queryRequest.getPage();
        long pageSize = queryRequest.getPageSize();
        queryRequest.setUserId(loginUser.userId());
        Page<RednoteNote> page = rednoteNoteService.page(new Page<>(current, pageSize),
                rednoteNoteService.getQueryWrapper(queryRequest, loginUser));
        List<RednoteNoteVO> voList = rednoteNoteService.getRednoteVO(page.getRecords());
        Page<RednoteNoteVO> voPage = new Page<>(current, pageSize, page.getTotalRow());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }

    /**
     * 用户端根据 id 查看本人小红书创作详情。
     */
    @GetMapping("/get")
    public BaseResponse<RednoteNoteVO> getMyRednoteById(@RequestParam @Positive(message = "小红书笔记 id 必须大于 0") long id) {
        RednoteNote note = rednoteNoteService.getOwnedRednote(id, LoginUserContext.getLoginUser());
        return ResultUtils.success(rednoteNoteService.getRednoteVO(note));
    }

    /**
     * 用户端根据 taskId 查看本人小红书创作详情，便于前端刷新后恢复页面。
     */
    @GetMapping("/detail/{taskId}")
    public BaseResponse<RednoteNoteVO> getMyRednoteByTaskId(@PathVariable String taskId) {
        RednoteNote note = rednoteNoteService.getOwnedRednoteByTaskId(taskId, LoginUserContext.getLoginUser());
        return ResultUtils.success(rednoteNoteService.getRednoteVO(note));
    }

    /**
     * 失败任务重新生成，沿用原始 content 从 SearchAgent 重新开始。
     */
    @PostMapping("/retry/{taskId}")
    public BaseResponse<Boolean> retryFailedRednote(@PathVariable String taskId) {
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        RednoteNote note = rednoteNoteService.getOwnedRednoteByTaskId(taskId, loginUser);
        ThrowUtils.throwIf(!RednoteStatusEnum.FAILED.getValue().equals(note.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有失败任务可以重新生成");
        return ResultUtils.success(rednoteWorkflowFacade.retryFailedNode(taskId, loginUser));
    }

    /**
     * 建立小红书生成进度 SSE 连接。
     */
    @GetMapping(value = "/progress/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getProgress(@PathVariable String taskId) {
        return createProgressEmitter(taskId);
    }

    /**
     * 建立兼容命名的 SSE 连接。
     */
    @GetMapping(value = "/sse/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse(@PathVariable String taskId) {
        return createProgressEmitter(taskId);
    }

    /**
     * 下载本人小红书任务已生成图片，后端代理 OSS 请求，避免浏览器 CORS 限制。
     */
    @GetMapping("/image/download")
    public ResponseEntity<byte[]> downloadRednoteImage(@RequestParam String taskId, @RequestParam String imageUrl) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(imageUrl), ErrorCode.PARAMS_ERROR, "图片地址不能为空");
        RednoteNote note = rednoteNoteService.getOwnedRednoteByTaskId(taskId, LoginUserContext.getLoginUser());
        assertImageBelongsToRednote(note, imageUrl);

        WorkflowImageData imageData = remoteImageDownloader.download(imageUrl);
        String fileName = "rednote-%s-image%s".formatted(taskId, imageData.getExtension());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(imageData.getMimeType()))
                .body(imageData.getBytes());
    }

    /**
     * 创建 SSE 连接并立即推送当前任务快照，避免前端错过早期生成事件。
     */
    private SseEmitter createProgressEmitter(String taskId) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        LoginUserInfo loginUser = LoginUserContext.getLoginUser();
        rednoteNoteService.getOwnedRednoteByTaskId(taskId, loginUser);
        SseEmitter emitter = sseEmitterManager.createEmitter(taskId);
        RednoteNote latestNote = rednoteNoteService.getOwnedRednoteByTaskId(taskId, loginUser);
        RednoteNoteVO noteVO = rednoteNoteService.getRednoteVO(latestNote);
        WorkflowSseMessage<RednoteNoteVO> message = WorkflowSseMessage.of("PROGRESS", "小红书任务进度", noteVO);
        sseEmitterManager.send(taskId, JSONUtil.toJsonStr(message));
        return emitter;
    }

    /**
     * 下载接口只允许访问当前 rednote 任务已经保存过的图片 URL，避免变成任意远程地址代理。
     */
    private void assertImageBelongsToRednote(RednoteNote note, String imageUrl) {
        if (imageUrl.equals(note.getCoverImage())) {
            return;
        }
        if (StringUtils.isBlank(note.getImages())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片不属于当前小红书任务");
        }
        List<RednoteWorkflowState.RednoteImageResult> images = JSONUtil.toList(note.getImages(),
                RednoteWorkflowState.RednoteImageResult.class);
        boolean matched = images.stream()
                .anyMatch(image -> image != null && imageUrl.equals(image.getUrl()));
        if (!matched) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "图片不属于当前小红书任务");
        }
    }
}
