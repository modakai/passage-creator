package com.sakura.passage_creator.rednote.config;

import com.sakura.passage_creator.prompt.api.PromptTemplateService;
import com.sakura.passage_creator.prompt.model.dto.PromptTemplateAddRequest;
import com.sakura.passage_creator.rednote.agent.RednoteContentAgent;
import com.sakura.passage_creator.rednote.agent.RednoteCoverImagePromptAgent;
import com.sakura.passage_creator.rednote.agent.RednoteNormalImagePromptAgent;
import com.sakura.passage_creator.rednote.agent.RednoteSearchAgent;
import com.sakura.passage_creator.rednote.constant.RednotePromptConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 小红书默认 Prompt 模板初始化器，只在模板 key 从未初始化时写入当前 Agent 默认文本。
 */
@Slf4j
@Component
public class RednotePromptTemplateInitializer implements ApplicationRunner {

    private static final String DEFAULT_VERSION = "1.0.0";

    private static final String OPERATOR = "system";

    private final PromptTemplateService promptTemplateService;

    public RednotePromptTemplateInitializer(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    /**
     * 应用启动后补齐 rednote 默认 Prompt，已有任意版本时不覆盖管理员维护的模板。
     */
    @Override
    public void run(ApplicationArguments args) {
        List<DefaultTemplate> templates = List.of(
                new DefaultTemplate(RednotePromptConstant.SEARCH_SYSTEM_KEY, RednoteSearchAgent.SYSTEM_PROMPT, null,
                        "默认小红书搜索整理系统 Prompt"),
                new DefaultTemplate(RednotePromptConstant.SEARCH_USER_KEY, RednotePromptConstant.SEARCH_USER_PROMPT,
                        RednotePromptConstant.SEARCH_USER_VARIABLE_SCHEMA, "默认小红书搜索整理用户指令"),
                new DefaultTemplate(RednotePromptConstant.CONTENT_SYSTEM_KEY, RednoteContentAgent.SYSTEM_PROMPT, null,
                        "默认小红书文案生成系统 Prompt"),
                new DefaultTemplate(RednotePromptConstant.CONTENT_USER_KEY, RednotePromptConstant.CONTENT_USER_PROMPT,
                        RednotePromptConstant.CONTENT_USER_VARIABLE_SCHEMA, "默认小红书文案生成用户指令"),
                new DefaultTemplate(RednotePromptConstant.NORMAL_IMAGE_PROMPT_SYSTEM_KEY,
                        RednoteNormalImagePromptAgent.SYSTEM_PROMPT, null, "默认小红书普通配图提示词系统 Prompt"),
                new DefaultTemplate(RednotePromptConstant.NORMAL_IMAGE_PROMPT_USER_KEY,
                        RednotePromptConstant.NORMAL_IMAGE_PROMPT_USER_PROMPT,
                        RednotePromptConstant.NORMAL_IMAGE_PROMPT_USER_VARIABLE_SCHEMA, "默认小红书普通配图提示词用户指令"),
                new DefaultTemplate(RednotePromptConstant.COVER_IMAGE_PROMPT_SYSTEM_KEY,
                        RednoteCoverImagePromptAgent.SYSTEM_PROMPT, null, "默认小红书封面提示词系统 Prompt"),
                new DefaultTemplate(RednotePromptConstant.COVER_IMAGE_PROMPT_USER_KEY,
                        RednotePromptConstant.COVER_IMAGE_PROMPT_USER_PROMPT,
                        RednotePromptConstant.COVER_IMAGE_PROMPT_USER_VARIABLE_SCHEMA, "默认小红书封面提示词用户指令")
        );
        for (DefaultTemplate template : templates) {
            initializeIfAbsent(template);
        }
    }

    /**
     * 仅当同 key 同环境没有任何版本时写入默认模板，避免启动时覆盖后台发布结果。
     */
    private void initializeIfAbsent(DefaultTemplate template) {
        try {
            if (!promptTemplateService.listTemplateVersions(template.templateKey(),
                    PromptTemplateService.DEFAULT_ENVIRONMENT).isEmpty()) {
                return;
            }
            Long id = promptTemplateService.addTemplate(buildAddRequest(template), OPERATOR);
            promptTemplateService.publishTemplate(id, OPERATOR);
        } catch (RuntimeException e) {
            log.warn("初始化 rednote 默认 Prompt 模板失败, templateKey={}", template.templateKey(), e);
        }
    }

    /**
     * 构造 Prompt 模板新增请求，默认版本使用 production 环境。
     */
    private PromptTemplateAddRequest buildAddRequest(DefaultTemplate template) {
        PromptTemplateAddRequest request = new PromptTemplateAddRequest();
        request.setTemplateKey(template.templateKey());
        request.setVersion(DEFAULT_VERSION);
        request.setContent(template.content());
        request.setVariablesSchema(template.variablesSchema());
        request.setDescription(template.description());
        request.setEnvironment(PromptTemplateService.DEFAULT_ENVIRONMENT);
        return request;
    }

    /**
     * 默认模板定义。
     */
    private record DefaultTemplate(String templateKey, String content, String variablesSchema, String description) {
    }
}
