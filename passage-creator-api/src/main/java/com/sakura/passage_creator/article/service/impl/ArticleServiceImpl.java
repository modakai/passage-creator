package com.sakura.passage_creator.article.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.constant.ArticleStatusConstant;
import com.sakura.passage_creator.article.model.dto.ArticleAddRequest;
import com.sakura.passage_creator.article.model.dto.ArticleQueryRequest;
import com.sakura.passage_creator.article.model.dto.ArticleUpdateRequest;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.model.vo.ArticleVO;
import com.sakura.passage_creator.article.repository.ArticleMapper;
import com.sakura.passage_creator.article.service.ArticleService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserContext;
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

import static com.sakura.passage_creator.article.model.entity.table.ArticleTableDef.ARTICLE;

/**
 * 文章服务实现。
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    /**
     * MapStruct Plus 转换器。
     */
    private final Converter converter;

    public ArticleServiceImpl(Converter converter) {
        this.converter = converter;
    }

    @Override
    public Long addArticle(ArticleAddRequest request, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = converter.convert(request, Article.class);
        article.setTaskId(UUID.randomUUID().toString());
        article.setUserId(loginUser.userId());
        fillDefaultFields(article);
        validArticle(article);
        boolean result = this.save(article);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return article.getId();
    }

    @Override
    public boolean updateArticle(ArticleUpdateRequest request, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article oldArticle = getAccessibleArticle(request.getId(), loginUser);
        Article article = converter.convert(request, Article.class);
        article.setTaskId(oldArticle.getTaskId());
        article.setUserId(oldArticle.getUserId());
        fillDefaultFields(article);
        validArticle(article);
        if (ArticleStatusConstant.COMPLETED.equals(article.getStatus()) && oldArticle.getCompletedTime() == null) {
            article.setCompletedTime(LocalDateTime.now());
        }
        return this.updateById(article);
    }

    @Override
    public boolean removeArticle(Long id, LoginUserInfo loginUser) {
        Article article = getAccessibleArticle(id, loginUser);
        return this.removeById(article.getId());
    }

    @Override
    public Article getAccessibleArticle(Long id, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Article article = this.getById(id);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);
        assertArticleAccessible(article, loginUser);
        return article;
    }

    @Override
    public Article getAccessibleArticleByTaskId(String taskId, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        Article article = this.getOne(QueryWrapper.create()
                .where(ARTICLE.TASK_ID.eq(taskId)));
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);
        assertArticleAccessible(article, loginUser);
        return article;
    }

    @Override
    public Article getOwnedArticle(Long id, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 用户端只允许读取本人创建的任务，管理员进入前台时也不扩大可见范围。
        Article article = this.getOne(QueryWrapper.create()
                .where(ARTICLE.ID.eq(id))
                .and(ARTICLE.USER_ID.eq(loginUser.userId())));
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);
        return article;
    }

    @Override
    public Article getOwnedArticleByTaskId(String taskId, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        // 用户端恢复流程必须绑定本人 taskId，避免管理员角色绕过前台边界读取他人任务。
        Article article = this.getOne(QueryWrapper.create()
                .where(ARTICLE.TASK_ID.eq(taskId))
                .and(ARTICLE.USER_ID.eq(loginUser.userId())));
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);
        return article;
    }

    @Override
    public QueryWrapper getQueryWrapper(ArticleQueryRequest queryRequest, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(ARTICLE.ID.eq(queryRequest.getId(), queryRequest.getId() != null));
        queryWrapper.and(ARTICLE.TASK_ID.eq(queryRequest.getTaskId(), StringUtils.isNotBlank(queryRequest.getTaskId())));
        queryWrapper.and(ARTICLE.TOPIC.like(queryRequest.getTopic(), StringUtils.isNotBlank(queryRequest.getTopic())));
        queryWrapper.and(ARTICLE.STATUS.eq(queryRequest.getStatus(), StringUtils.isNotBlank(queryRequest.getStatus())));
        queryWrapper.and(ARTICLE.MAIN_TITLE.like(queryRequest.getTitle(), StringUtils.isNotBlank(queryRequest.getTitle()))
                .or(ARTICLE.SUB_TITLE.like(queryRequest.getTitle(), StringUtils.isNotBlank(queryRequest.getTitle()))));
        if (isAdmin(loginUser)) {
            queryWrapper.and(ARTICLE.USER_ID.eq(queryRequest.getUserId(), queryRequest.getUserId() != null));
        } else {
            // 普通用户列表查询强制限定为本人数据，不能信任客户端传入 userId。
            queryWrapper.and(ARTICLE.USER_ID.eq(loginUser.userId()));
        }
        QueryColumn sortColumn = resolveSortColumn(queryRequest.getSortField());
        if (sortColumn != null) {
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(queryRequest.getSortOrder()));
        } else {
            queryWrapper.orderBy(ARTICLE.CREATE_TIME, false);
            queryWrapper.orderBy(ARTICLE.ID, false);
        }
        return queryWrapper;
    }

    @Override
    public void assertArticleAccessible(Article article, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);
        if (isAdmin(loginUser)) {
            return;
        }
        ThrowUtils.throwIf(!loginUser.userId().equals(article.getUserId()), ErrorCode.NO_AUTH_ERROR);
    }

    @Override
    public ArticleVO getArticleVO(Article article) {
        if (article == null) {
            return null;
        }
        return converter.convert(article, ArticleVO.class);
    }

    @Override
    public List<ArticleVO> getArticleVO(List<Article> articleList) {
        if (CollUtil.isEmpty(articleList)) {
            return new ArrayList<>();
        }
        return articleList.stream().map(this::getArticleVO).collect(Collectors.toList());
    }

    @Override
    public String createArticle(String topic, LoginUserInfo loginUser) {
        return createArticle(topic, List.of(), loginUser);
    }

    @Override
    public String createArticle(String topic, List<String> enabledImageMethods, LoginUserInfo loginUser) {
        assertLogin(loginUser);
        ThrowUtils.throwIf(StringUtils.isBlank(topic), ErrorCode.PARAMS_ERROR, "文章选题不能为空");
        ThrowUtils.throwIf(topic.length() > 500, ErrorCode.PARAMS_ERROR, "文章选题不能超过 500 个字符");
        // 使用 UUID 生成对外任务 id，避免暴露数据库自增主键。
        String taskId = UUID.randomUUID().toString();
        Article article = new Article();
        article.setUserId(loginUser.userId());
        article.setTaskId(taskId);
        article.setTopic(topic);
        article.setStatus(ArticleStatusEnum.PENDING.getValue());
        article.setPhase(ArticlePhaseEnum.PENDING.getValue());
        article.setEnabledImageMethods(JSONUtil.toJsonStr(normalizeEnabledImageMethods(enabledImageMethods)));

        save(article);

        return taskId;
    }

    /**
     * 清洗客户端传入的配图方式，只保存后端支持且允许用户主动选择的方式。
     */
    private List<String> normalizeEnabledImageMethods(List<String> enabledImageMethods) {
        if (enabledImageMethods == null || enabledImageMethods.isEmpty()) {
            return ImageMethodEnum.userSelectableMethods().stream()
                    .map(ImageMethodEnum::getValue)
                    .toList();
        }
        List<String> normalized = enabledImageMethods.stream()
                .map(ImageMethodEnum::getByValue)
                .filter(method -> method != null && !method.isFallback())
                .map(ImageMethodEnum::getValue)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            return List.of(ImageMethodEnum.getDefaultAiMethod().getValue());
        }
        return normalized;
    }

    @Override
    public Boolean updateStatus(ArticleStatusEnum articleStatusEnum, String taskId) {
        return updateChain()
                .set(ARTICLE.STATUS, articleStatusEnum.getValue())
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    @Override
    public boolean updatePhase(ArticlePhaseEnum articlePhaseEnum, String taskId) {
        return updateChain()
                .set(ARTICLE.PHASE, articlePhaseEnum.getValue())
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    @Override
    public Boolean saveTitleOptions(List<ArticleState.TitleOption> titleOptions, String taskId) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        ThrowUtils.throwIf(CollUtil.isEmpty(titleOptions), ErrorCode.PARAMS_ERROR, "标题方案不能为空");
        // MySQL json 字段由数据库校验合法性，应用层统一以 JSON 字符串写入。
        String titleOptionsJson = JSONUtil.toJsonStr(titleOptions);
        return updateChain()
                .set(ARTICLE.TITLE_OPTIONS, titleOptionsJson)
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    @Override
    public boolean confirmTitle(Article article) {
        Article entity = this.getOne(QueryWrapper.create()
                .where(ARTICLE.TASK_ID.eq(article.getTaskId()))
                .where(ARTICLE.USER_ID.eq(article.getUserId())));

        // 校验是否处于 标题待选择状态
        if (entity == null) return false;

        String phase = entity.getPhase();
        if (!ArticlePhaseEnum.TITLE_SELECTING.getValue().equals(phase)) return false;

        // 保存
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());
        article.setStatus(ArticleStatusEnum.PROCESSING.getValue());
        article.setUserId(null);
        article.setId(entity.getId());

        return updateById(article);
    }

    @Override
    public Boolean saveOutline(ArticleState.OutlineResult outline, String taskId) {
        return updateChain()
                .set(ARTICLE.OUTLINE, JSONUtil.toJsonStr(outline))
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    @Override
    public boolean confirmOutline(String taskId, ArticleState.OutlineResult outline) {
        Long userId = LoginUserContext.getLoginUser().userId();
        // 校验状态
        Article entity = this.getOne(QueryWrapper.create()
                .where(ARTICLE.TASK_ID.eq(taskId))
                .where(ARTICLE.USER_ID.eq(userId)));

        // 校验是否处于 标题待选择状态
        if (entity == null) return false;

        String phase = entity.getPhase();
        if (!ArticlePhaseEnum.OUTLINE_EDITING.getValue().equals(phase)) return false;

        // 更新
        return saveOutline(outline, taskId);
    }

    @Override
    public boolean completeContent(String taskId, String content) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "正文内容不能为空");
        // 正文生成完成时同步写入 fullContent，后续配图流程启用后可再覆盖 fullContent。
        return updateChain()
                .set(ARTICLE.CONTENT, content)
                .set(ARTICLE.FULL_CONTENT, content)
                .set(ARTICLE.STATUS, ArticleStatusEnum.COMPLETED.getValue())
                .set(ARTICLE.PHASE, ArticlePhaseEnum.COMPLETED.getValue())
                .set(ARTICLE.ERROR_MESSAGE, null)
                .set(ARTICLE.COMPLETED_TIME, LocalDateTime.now())
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    @Override
    public boolean completeContentWithImages(String taskId, ArticleState state) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        ThrowUtils.throwIf(state == null || StringUtils.isBlank(state.getContent()),
                ErrorCode.PARAMS_ERROR, "正文内容不能为空");
        String fullContent = StringUtils.defaultIfBlank(state.getFullContent(), state.getContent());

        // 配图信息和完整图文一起落库，保证刷新页面后能恢复最终生成结果。
        return updateChain()
                .set(ARTICLE.CONTENT, state.getContent())
                .set(ARTICLE.FULL_CONTENT, fullContent)
                .set(ARTICLE.COVER_IMAGE, state.getCoverImage())
                .set(ARTICLE.IMAGES, JSONUtil.toJsonStr(state.getImages()))
                .set(ARTICLE.STATUS, ArticleStatusEnum.COMPLETED.getValue())
                .set(ARTICLE.PHASE, ArticlePhaseEnum.COMPLETED.getValue())
                .set(ARTICLE.ERROR_MESSAGE, null)
                .set(ARTICLE.COMPLETED_TIME, LocalDateTime.now())
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    @Override
    public boolean markFailed(String taskId, String errorMessage) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务 id 不能为空");
        // 失败原因写入数据库，方便前端刷新后仍能展示错误上下文。
        return updateChain()
                .set(ARTICLE.STATUS, ArticleStatusEnum.FAILED.getValue())
                .set(ARTICLE.PHASE, ArticlePhaseEnum.FAILED.getValue())
                .set(ARTICLE.ERROR_MESSAGE, StringUtils.defaultIfBlank(errorMessage, "文章生成失败"))
                .where(ARTICLE.TASK_ID.eq(taskId))
                .update();
    }

    /**
     * 填充新增和更新时的默认字段。
     */
    private void fillDefaultFields(Article article) {
        if (StringUtils.isBlank(article.getStatus())) {
            article.setStatus(ArticleStatusConstant.PENDING);
        }
        if (StringUtils.isBlank(article.getFullContent())) {
            article.setFullContent(article.getContent());
        }
    }

    /**
     * 校验文章核心字段和状态值。
     */
    private void validArticle(Article article) {
        if (article == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ThrowUtils.throwIf(article.getUserId() == null || article.getUserId() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isBlank(article.getTaskId()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StringUtils.isBlank(article.getTopic()), ErrorCode.PARAMS_ERROR, "文章选题不能为空");
        ThrowUtils.throwIf(article.getTopic().length() > 500, ErrorCode.PARAMS_ERROR, "文章选题不能超过 500 个字符");
        ThrowUtils.throwIf(!isValidStatus(article.getStatus()), ErrorCode.PARAMS_ERROR, "文章状态非法");
    }

    /**
     * 判断状态是否属于文章表支持的固定集合。
     */
    private boolean isValidStatus(String status) {
        return ArticleStatusConstant.PENDING.equals(status)
                || ArticleStatusConstant.PROCESSING.equals(status)
                || ArticleStatusConstant.COMPLETED.equals(status)
                || ArticleStatusConstant.FAILED.equals(status);
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
     * 将客户端排序字段转换为文章表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> ARTICLE.ID;
            case "taskId", "task_id" -> ARTICLE.TASK_ID;
            case "userId", "user_id" -> ARTICLE.USER_ID;
            case "topic" -> ARTICLE.TOPIC;
            case "status" -> ARTICLE.STATUS;
            case "createTime", "create_time" -> ARTICLE.CREATE_TIME;
            case "completedTime", "completed_time" -> ARTICLE.COMPLETED_TIME;
            case "updateTime", "update_time" -> ARTICLE.UPDATE_TIME;
            default -> null;
        };
    }
}
