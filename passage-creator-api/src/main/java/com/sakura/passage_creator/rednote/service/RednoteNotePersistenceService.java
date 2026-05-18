package com.sakura.passage_creator.rednote.service;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.rednote.model.dto.RednoteQueryRequest;
import com.sakura.passage_creator.rednote.model.entity.RednoteNote;
import com.sakura.passage_creator.rednote.model.enums.RednotePhaseEnum;
import com.sakura.passage_creator.rednote.model.enums.RednoteStatusEnum;
import com.sakura.passage_creator.rednote.model.vo.RednoteNoteVO;
import com.sakura.passage_creator.rednote.repository.RednoteNoteMapper;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.rednote.model.entity.table.RednoteNoteTableDef.REDNOTE_NOTE;

/**
 * 小红书笔记持久化服务，封装 Hook 场景下的局部字段更新。
 */
@Service
public class RednoteNotePersistenceService extends ServiceImpl<RednoteNoteMapper, RednoteNote> {

    /**
     * MapStruct Plus 转换器，用于实体到前端 VO 的稳定映射。
     */
    private final Converter converter;

    public RednoteNotePersistenceService(Converter converter) {
        this.converter = converter;
    }

    /**
     * 创建 rednote 业务记录，返回对外 workflow taskId。
     */
    public String createRednote(String content, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "小红书创作需求不能为空");
        ThrowUtils.throwIf(content.length() > 2000, ErrorCode.PARAMS_ERROR, "小红书创作需求不能超过 2000 个字符");
        String taskId = UUID.randomUUID().toString();
        RednoteNote note = new RednoteNote();
        note.setTaskId(taskId);
        note.setUserId(loginUser.userId());
        note.setContent(content);
        note.setStatus(RednoteStatusEnum.PENDING.getValue());
        note.setPhase(RednotePhaseEnum.PENDING.getValue());
        boolean saved = save(note);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建小红书任务失败");
        return taskId;
    }

    /**
     * 根据 taskId 查询当前用户自己的 rednote 记录。
     */
    public RednoteNote getOwnedRednoteByTaskId(String taskId, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        RednoteNote note = getOne(QueryWrapper.create()
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .and(REDNOTE_NOTE.USER_ID.eq(loginUser.userId())));
        ThrowUtils.throwIf(note == null, ErrorCode.NOT_FOUND_ERROR);
        return note;
    }

    /**
     * workflow 后台线程按 taskId 查询 rednote 记录，用于节点间状态兜底恢复。
     */
    public RednoteNote getRednoteByTaskId(String taskId) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        RednoteNote note = getOne(QueryWrapper.create()
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId)));
        ThrowUtils.throwIf(note == null, ErrorCode.NOT_FOUND_ERROR, "小红书任务不存在");
        return note;
    }

    /**
     * 根据 id 查询当前用户自己的 rednote 记录。
     */
    public RednoteNote getOwnedRednote(Long id, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "小红书笔记 id 非法");
        RednoteNote note = getOne(QueryWrapper.create()
                .where(REDNOTE_NOTE.ID.eq(id))
                .and(REDNOTE_NOTE.USER_ID.eq(loginUser.userId())));
        ThrowUtils.throwIf(note == null, ErrorCode.NOT_FOUND_ERROR);
        return note;
    }

    /**
     * 构建用户端 rednote 分页查询条件。
     */
    public QueryWrapper getQueryWrapper(RednoteQueryRequest queryRequest, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(REDNOTE_NOTE.ID.eq(queryRequest.getId(), queryRequest.getId() != null));
        queryWrapper.and(REDNOTE_NOTE.TASK_ID.eq(queryRequest.getTaskId(), StringUtils.isNotBlank(queryRequest.getTaskId())));
        queryWrapper.and(REDNOTE_NOTE.CONTENT.like(queryRequest.getContent(), StringUtils.isNotBlank(queryRequest.getContent())));
        queryWrapper.and(REDNOTE_NOTE.SUBJECT.like(queryRequest.getSubject(), StringUtils.isNotBlank(queryRequest.getSubject())));
        queryWrapper.and(REDNOTE_NOTE.CONTENT_LENGTH.eq(queryRequest.getContentLength(),
                StringUtils.isNotBlank(queryRequest.getContentLength())));
        queryWrapper.and(REDNOTE_NOTE.STATUS.eq(queryRequest.getStatus(), StringUtils.isNotBlank(queryRequest.getStatus())));
        queryWrapper.and(REDNOTE_NOTE.PHASE.eq(queryRequest.getPhase(), StringUtils.isNotBlank(queryRequest.getPhase())));
        queryWrapper.and(REDNOTE_NOTE.CREATE_TIME.ge(queryRequest.getStartTime(), queryRequest.getStartTime() != null));
        queryWrapper.and(REDNOTE_NOTE.CREATE_TIME.le(queryRequest.getEndTime(), queryRequest.getEndTime() != null));
        if (isAdmin(loginUser)) {
            queryWrapper.and(REDNOTE_NOTE.USER_ID.eq(queryRequest.getUserId(), queryRequest.getUserId() != null));
        } else {
            // 用户端列表必须限定本人数据，不能信任客户端传入 userId。
            queryWrapper.and(REDNOTE_NOTE.USER_ID.eq(loginUser.userId()));
        }
        QueryColumn sortColumn = resolveSortColumn(queryRequest.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(queryRequest.getSortOrder()));
        } else {
            queryWrapper.orderBy(REDNOTE_NOTE.CREATE_TIME, false);
            queryWrapper.orderBy(REDNOTE_NOTE.ID, false);
        }
        return queryWrapper;
    }

    /**
     * 转换 rednote 展示对象，并补充状态和阶段中文描述。
     */
    public RednoteNoteVO getRednoteVO(RednoteNote note) {
        if (note == null) {
            return null;
        }
        RednoteNoteVO vo = converter.convert(note, RednoteNoteVO.class);
        RednoteStatusEnum statusEnum = RednoteStatusEnum.getByValue(note.getStatus());
        RednotePhaseEnum phaseEnum = RednotePhaseEnum.getByValue(note.getPhase());
        vo.setStatusLabel(statusEnum == null ? note.getStatus() : statusEnum.getDescription());
        vo.setPhaseLabel(phaseEnum == null ? note.getPhase() : phaseEnum.getDescription());
        return vo;
    }

    /**
     * 批量转换 rednote 展示对象。
     */
    public List<RednoteNoteVO> getRednoteVO(List<RednoteNote> noteList) {
        if (CollUtil.isEmpty(noteList)) {
            return new ArrayList<>();
        }
        return noteList.stream().map(this::getRednoteVO).collect(Collectors.toList());
    }

    /**
     * 按 taskId 更新任务状态和阶段，不覆盖其他并行节点已经写入的业务字段。
     */
    public boolean markPhase(String taskId, String status, String phase) {
        return updateChain()
                .set(REDNOTE_NOTE.STATUS, status)
                .set(REDNOTE_NOTE.PHASE, phase)
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 保存普通配图提示词，供后续图片生成节点并行消费。
     */
    public boolean saveNormalImagePrompts(String taskId, String imagePrompts, String status, String phase) {
        return updateChain()
                .set(REDNOTE_NOTE.IMAGE_PROMPTS, imagePrompts)
                .set(REDNOTE_NOTE.STATUS, status)
                .set(REDNOTE_NOTE.PHASE, phase)
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 保存封面标题和封面提示词，避免并行分支覆盖普通图提示词。
     */
    public boolean saveCoverImagePrompt(String taskId, String coverTitle, String coverPrompt, String status, String phase) {
        return updateChain()
                .set(REDNOTE_NOTE.COVER_TITLE, coverTitle)
                .set(REDNOTE_NOTE.COVER_PROMPT, coverPrompt)
                .set(REDNOTE_NOTE.STATUS, status)
                .set(REDNOTE_NOTE.PHASE, phase)
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 保存普通配图生成结果，图片生成分支只写 images 字段。
     */
    public boolean saveNormalImages(String taskId, String images, String status, String phase) {
        return updateChain()
                .set(REDNOTE_NOTE.IMAGES, images)
                .set(REDNOTE_NOTE.STATUS, status)
                .set(REDNOTE_NOTE.PHASE, phase)
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 保存封面图 URL，封面图分支只写 cover_image 字段。
     */
    public boolean saveCoverImage(String taskId, String coverImage, String status, String phase) {
        return updateChain()
                .set(REDNOTE_NOTE.COVER_IMAGE, coverImage)
                .set(REDNOTE_NOTE.STATUS, status)
                .set(REDNOTE_NOTE.PHASE, phase)
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 标记 rednote workflow 完成，最终节点统一负责结束流程，避免并行分支提前置完成。
     */
    public boolean markCompleted(String taskId) {
        return updateChain()
                .set(REDNOTE_NOTE.STATUS, RednoteStatusEnum.COMPLETED.getValue())
                .set(REDNOTE_NOTE.PHASE, RednotePhaseEnum.COMPLETED.getValue())
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.COMPLETED_TIME, LocalDateTime.now())
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 标记 rednote workflow 失败，失败原因会在详情页展示。
     */
    public boolean markFailed(String taskId, String errorMessage) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        return updateChain()
                .set(REDNOTE_NOTE.STATUS, RednoteStatusEnum.FAILED.getValue())
                .set(REDNOTE_NOTE.PHASE, RednotePhaseEnum.FAILED.getValue())
                .set(REDNOTE_NOTE.ERROR_MESSAGE, StringUtils.defaultIfBlank(errorMessage, "小红书生成失败"))
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 失败任务重新生成前，将业务状态重置成可执行状态，保留原始 content。
     */
    public boolean resetForRetry(String taskId) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        return updateChain()
                .set(REDNOTE_NOTE.STATUS, RednoteStatusEnum.PENDING.getValue())
                .set(REDNOTE_NOTE.PHASE, RednotePhaseEnum.PENDING.getValue())
                .set(REDNOTE_NOTE.ERROR_MESSAGE, null)
                .set(REDNOTE_NOTE.COMPLETED_TIME, null)
                .set(REDNOTE_NOTE.UPDATE_TIME, LocalDateTime.now())
                .where(REDNOTE_NOTE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 校验当前请求已经登录。
     */
    private void assertLogin(LoginUserInfo loginUser) {
        ThrowUtils.throwIf(loginUser == null || loginUser.userId() == null, ErrorCode.NOT_LOGIN_ERROR);
    }

    /**
     * 判断当前登录用户是否为管理员。
     */
    private boolean isAdmin(LoginUserInfo loginUser) {
        return UserConstant.ADMIN_ROLE.equals(loginUser.userRole());
    }

    /**
     * 将客户端排序字段转换为 rednote 表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> REDNOTE_NOTE.ID;
            case "taskId", "task_id" -> REDNOTE_NOTE.TASK_ID;
            case "userId", "user_id" -> REDNOTE_NOTE.USER_ID;
            case "content" -> REDNOTE_NOTE.CONTENT;
            case "subject" -> REDNOTE_NOTE.SUBJECT;
            case "status" -> REDNOTE_NOTE.STATUS;
            case "phase" -> REDNOTE_NOTE.PHASE;
            case "createTime", "create_time" -> REDNOTE_NOTE.CREATE_TIME;
            case "completedTime", "completed_time" -> REDNOTE_NOTE.COMPLETED_TIME;
            case "updateTime", "update_time" -> REDNOTE_NOTE.UPDATE_TIME;
            default -> null;
        };
    }
}
