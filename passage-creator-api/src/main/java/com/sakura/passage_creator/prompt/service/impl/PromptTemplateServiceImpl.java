package com.sakura.passage_creator.prompt.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateAddRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateQueryRequest;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateUpdateRequest;
import com.sakura.passage_creator.prompt.model.entity.PromptTemplate;
import com.sakura.passage_creator.prompt.model.enums.PromptTemplateStatusEnum;
import com.sakura.passage_creator.prompt.model.vo.PromptTemplateVO;
import com.sakura.passage_creator.prompt.repository.PromptTemplateMapper;
import com.sakura.passage_creator.prompt.service.PromptTemplateJsonSchemaNormalizer;
import com.sakura.passage_creator.prompt.api.PromptTemplateRenderResult;
import com.sakura.passage_creator.prompt.service.PromptTemplateRenderer;
import com.sakura.passage_creator.prompt.api.PromptTemplateService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.prompt.model.entity.table.PromptTemplateTableDef.PROMPT_TEMPLATE;

/**
 * Prompt 模板版本服务实现。
 */
@Service
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate>
        implements PromptTemplateService {

    /**
     * 模板标识格式，避免运行时出现难以治理的随意 key。
     */
    private static final Pattern TEMPLATE_KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_.-]*$");

    /**
     * 环境格式，当前只做轻量约束以兼容 production/staging/dev。
     */
    private static final Pattern ENVIRONMENT_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

    /**
     * ACTIVE 模板本地缓存，发布或刷新时主动失效。
     */
    private final Map<String, PromptTemplate> activeTemplateCache = new ConcurrentHashMap<>();

    /**
     * Prompt 模板渲染器。
     */
    private final PromptTemplateRenderer renderer;

    /**
     * 变量定义 JSON 归一化器。
     */
    private final PromptTemplateJsonSchemaNormalizer schemaNormalizer;

    public PromptTemplateServiceImpl(PromptTemplateRenderer renderer,
            PromptTemplateJsonSchemaNormalizer schemaNormalizer) {
        this.renderer = renderer;
        this.schemaNormalizer = schemaNormalizer;
    }

    @Override
    public Long addTemplate(PromptTemplateAddRequest request, String operator) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        PromptTemplate template = new PromptTemplate();
        template.setTemplateKey(StringUtils.trim(request.getTemplateKey()));
        template.setVersion(StringUtils.trim(request.getVersion()));
        template.setContent(request.getContent());
        template.setVariablesSchema(schemaNormalizer.normalize(request.getVariablesSchema()));
        template.setDescription(request.getDescription());
        template.setEnvironment(resolveEnvironment(request.getEnvironment()));
        template.setStatus(PromptTemplateStatusEnum.DRAFT.getValue());
        template.setCreatedBy(operator);
        validTemplate(template);
        ThrowUtils.throwIf(existsVersion(template.getTemplateKey(), template.getVersion(), template.getEnvironment(), null),
                ErrorCode.PARAMS_ERROR, "同环境下模板版本已存在");
        boolean result = this.save(template);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return template.getId();
    }

    @Override
    public boolean updateTemplate(PromptTemplateUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        PromptTemplate oldTemplate = this.getById(request.getId());
        ThrowUtils.throwIf(oldTemplate == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!PromptTemplateStatusEnum.DRAFT.getValue().equals(oldTemplate.getStatus()),
                ErrorCode.OPERATION_ERROR, "只有草稿状态的 Prompt 模板允许编辑");
        if (request.getContent() != null) {
            oldTemplate.setContent(request.getContent());
        }
        // 更新表单允许主动清空变量定义，空值会被归一化为 NULL。
        oldTemplate.setVariablesSchema(schemaNormalizer.normalize(request.getVariablesSchema()));
        if (request.getDescription() != null) {
            oldTemplate.setDescription(request.getDescription());
        }
        validTemplate(oldTemplate);
        return this.updateById(oldTemplate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishTemplate(Long id, String operator) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        PromptTemplate template = this.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR);
        validTemplate(template);

        // 发布时先归档同 key + env 的旧 ACTIVE 版本，保证运行时解析唯一。
        this.updateChain()
                .set(PROMPT_TEMPLATE.STATUS, PromptTemplateStatusEnum.ARCHIVED.getValue())
                .where(PROMPT_TEMPLATE.TEMPLATE_KEY.eq(template.getTemplateKey()))
                .and(PROMPT_TEMPLATE.ENVIRONMENT.eq(template.getEnvironment()))
                .and(PROMPT_TEMPLATE.STATUS.eq(PromptTemplateStatusEnum.ACTIVE.getValue()))
                .and(PROMPT_TEMPLATE.ID.ne(template.getId()))
                .update();

        PromptTemplate publishEntity = new PromptTemplate();
        publishEntity.setId(template.getId());
        publishEntity.setStatus(PromptTemplateStatusEnum.ACTIVE.getValue());
        publishEntity.setPublishedBy(operator);
        publishEntity.setPublishedAt(LocalDateTime.now());
        boolean result = this.updateById(publishEntity);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        refreshTemplate(template.getTemplateKey(), template.getEnvironment());
        return true;
    }

    @Override
    public boolean archiveTemplate(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        PromptTemplate template = this.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(PromptTemplateStatusEnum.ACTIVE.getValue().equals(template.getStatus()),
                ErrorCode.OPERATION_ERROR, "ACTIVE Prompt 模板请先发布其他版本替换后再归档");
        PromptTemplate archiveEntity = new PromptTemplate();
        archiveEntity.setId(id);
        archiveEntity.setStatus(PromptTemplateStatusEnum.ARCHIVED.getValue());
        boolean result = this.updateById(archiveEntity);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        refreshTemplate(template.getTemplateKey(), template.getEnvironment());
        return true;
    }

    @Override
    public boolean deleteTemplate(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        PromptTemplate template = this.getById(id);
        ThrowUtils.throwIf(template == null, ErrorCode.NOT_FOUND_ERROR);
        // ACTIVE 是运行时读取版本，必须通过发布新版本完成替换，不能直接删除。
        ThrowUtils.throwIf(PromptTemplateStatusEnum.ACTIVE.getValue().equals(template.getStatus()),
                ErrorCode.OPERATION_ERROR, "ACTIVE Prompt 模板不允许删除");
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        refreshTemplate(template.getTemplateKey(), template.getEnvironment());
        return true;
    }

    @Override
    public List<String> listTemplateVersions(String templateKey, String environment) {
        ThrowUtils.throwIf(StringUtils.isBlank(templateKey), ErrorCode.PARAMS_ERROR, "模板标识不能为空");
        String resolvedEnvironment = resolveEnvironment(environment);
        return this.list(QueryWrapper.create()
                        .select(PROMPT_TEMPLATE.VERSION)
                        .where(PROMPT_TEMPLATE.TEMPLATE_KEY.eq(StringUtils.trim(templateKey)))
                        .and(PROMPT_TEMPLATE.ENVIRONMENT.eq(resolvedEnvironment)))
                .stream()
                .map(PromptTemplate::getVersion)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public boolean refreshTemplate(String templateKey, String environment) {
        ThrowUtils.throwIf(StringUtils.isBlank(templateKey), ErrorCode.PARAMS_ERROR, "模板标识不能为空");
        String resolvedEnvironment = resolveEnvironment(environment);
        activeTemplateCache.remove(buildCacheKey(templateKey, resolvedEnvironment));
        findActiveTemplate(templateKey, resolvedEnvironment);
        return true;
    }

    @Override
    public PromptTemplateRenderResult renderActive(String templateKey, String fallbackContent, String fallbackSchema,
            Map<String, ?> variables) {
        String environment = DEFAULT_ENVIRONMENT;
        PromptTemplate activeTemplate = findActiveTemplateSafely(templateKey, environment);
        if (activeTemplate == null) {
            String content = renderer.render(fallbackContent, fallbackSchema, variables);
            return new PromptTemplateRenderResult(null, templateKey, "fallback", environment, content, true);
        }
        String content = renderer.render(activeTemplate.getContent(), activeTemplate.getVariablesSchema(), variables);
        return new PromptTemplateRenderResult(activeTemplate.getId(), activeTemplate.getTemplateKey(),
                activeTemplate.getVersion(), activeTemplate.getEnvironment(), content, false);
    }

    @Override
    public QueryWrapper getQueryWrapper(PromptTemplateQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(PROMPT_TEMPLATE.ID.eq(request.getId(), request.getId() != null));
        queryWrapper.and(PROMPT_TEMPLATE.TEMPLATE_KEY.like(request.getTemplateKey(),
                StringUtils.isNotBlank(request.getTemplateKey())));
        queryWrapper.and(PROMPT_TEMPLATE.VERSION.eq(request.getVersion(), StringUtils.isNotBlank(request.getVersion())));
        queryWrapper.and(PROMPT_TEMPLATE.STATUS.eq(request.getStatus(), StringUtils.isNotBlank(request.getStatus())));
        queryWrapper.and(PROMPT_TEMPLATE.ENVIRONMENT.eq(request.getEnvironment(),
                StringUtils.isNotBlank(request.getEnvironment())));
        queryWrapper.orderBy(PROMPT_TEMPLATE.UPDATED_AT, false);
        queryWrapper.orderBy(PROMPT_TEMPLATE.ID, false);
        return queryWrapper;
    }

    @Override
    public PromptTemplateVO getTemplateVO(PromptTemplate template) {
        if (template == null) {
            return null;
        }
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setId(template.getId());
        vo.setTemplateKey(template.getTemplateKey());
        vo.setVersion(template.getVersion());
        vo.setContent(template.getContent());
        vo.setVariablesSchema(template.getVariablesSchema());
        vo.setDescription(template.getDescription());
        vo.setStatus(template.getStatus());
        vo.setEnvironment(template.getEnvironment());
        vo.setCreatedBy(template.getCreatedBy());
        vo.setPublishedBy(template.getPublishedBy());
        vo.setPublishedAt(template.getPublishedAt());
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());
        return vo;
    }

    @Override
    public List<PromptTemplateVO> getTemplateVO(List<PromptTemplate> templateList) {
        if (CollUtil.isEmpty(templateList)) {
            return new ArrayList<>();
        }
        return templateList.stream().map(this::getTemplateVO).collect(Collectors.toList());
    }

    /**
     * 查询 ACTIVE 模板，优先读取本地缓存。
     */
    private PromptTemplate findActiveTemplate(String templateKey, String environment) {
        return activeTemplateCache.computeIfAbsent(buildCacheKey(templateKey, environment),
                key -> this.getOne(QueryWrapper.create()
                        .where(PROMPT_TEMPLATE.TEMPLATE_KEY.eq(templateKey))
                        .and(PROMPT_TEMPLATE.ENVIRONMENT.eq(environment))
                        .and(PROMPT_TEMPLATE.STATUS.eq(PromptTemplateStatusEnum.ACTIVE.getValue()))
                        .orderBy(PROMPT_TEMPLATE.PUBLISHED_AT, false)
                        .orderBy(PROMPT_TEMPLATE.ID, false)));
    }

    /**
     * 安全查询 ACTIVE 模板，避免初始化 SQL 尚未执行时阻断 Agent 兜底运行。
     */
    private PromptTemplate findActiveTemplateSafely(String templateKey, String environment) {
        try {
            return findActiveTemplate(templateKey, environment);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 检查同环境下模板版本是否已存在。
     */
    private boolean existsVersion(String templateKey, String version, String environment, Long excludeId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(PROMPT_TEMPLATE.TEMPLATE_KEY.eq(templateKey))
                .and(PROMPT_TEMPLATE.VERSION.eq(version))
                .and(PROMPT_TEMPLATE.ENVIRONMENT.eq(environment));
        if (excludeId != null) {
            queryWrapper.and(PROMPT_TEMPLATE.ID.ne(excludeId));
        }
        return this.count(queryWrapper) > 0;
    }

    /**
     * 校验模板核心字段和变量声明。
     */
    private void validTemplate(PromptTemplate template) {
        ThrowUtils.throwIf(template == null, ErrorCode.PARAMS_ERROR);
        if (StringUtils.isAnyBlank(template.getTemplateKey(), template.getVersion(), template.getContent(),
                template.getEnvironment())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Prompt 模板核心字段不能为空");
        }
        ThrowUtils.throwIf(!TEMPLATE_KEY_PATTERN.matcher(template.getTemplateKey()).matches(),
                ErrorCode.PARAMS_ERROR, "模板标识格式错误");
        ThrowUtils.throwIf(!ENVIRONMENT_PATTERN.matcher(template.getEnvironment()).matches(),
                ErrorCode.PARAMS_ERROR, "运行环境格式错误");
        ThrowUtils.throwIf(!PromptTemplateStatusEnum.isValid(template.getStatus()),
                ErrorCode.PARAMS_ERROR, "Prompt 模板状态非法");
        renderer.validateTemplateVariables(template.getContent(), template.getVariablesSchema());
    }

    /**
     * 解析运行环境，未传时默认 production。
     */
    private String resolveEnvironment(String environment) {
        return StringUtils.defaultIfBlank(StringUtils.trim(environment), DEFAULT_ENVIRONMENT);
    }

    /**
     * 构造缓存键。
     */
    private String buildCacheKey(String templateKey, String environment) {
        return environment + "::" + templateKey;
    }
}
