package com.sakura.passage_creator.article.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.config.OpenAiImageProperties;
import com.sakura.passage_creator.article.image.service.ImageRequirementPolicy;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配图需求分析 Agent，负责在正文中插入占位符并生成 GPT_IMAGE 配图需求。
 */
@Component
@Slf4j
public class ImageAnalyzerAgent {

    /**
     * 结构化输出转换器，用于约束模型返回 Agent4Result JSON。
     */
    private final BeanOutputConverter<ArticleState.Agent4Result> outputConverter =
            new BeanOutputConverter<>(ArticleState.Agent4Result.class);

    /**
     * 聊天客户端，用于分析正文中的配图点位。
     */
    private final ChatClient chatClient;

    /**
     * 配图需求策略，控制数量并强制使用 GPT_IMAGE。
     */
    private final ImageRequirementPolicy imageRequirementPolicy;

    public ImageAnalyzerAgent(DashScopeApi dashScopeApi, OpenAiImageProperties imageProperties) {
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(DashScopeModel.ChatModel.QWEN3_MAX.value)
                        .temperature(0.1)
                        .maxToken(3000)
                        .topP(0.9)
                        // 配图需求需要稳定 JSON，避免夹杂解释文本导致后续无法解析。
                        .responseFormat(DashScopeResponseFormat.builder()
                                .type(DashScopeResponseFormat.Type.JSON_OBJECT)
                                .build())
                        .build())
                .build();
        this.chatClient = ChatClient.builder(chatModel).build();
        this.imageRequirementPolicy = new ImageRequirementPolicy(imageProperties.getMaxSectionImages());
    }

    /**
     * 分析正文配图需求并写回文章状态。
     *
     * @param state 文章生成状态
     */
    public void analyze(ArticleState state) {
        log.info("阶段4：开始分析配图需求, taskId={}", state.getTaskId());
        String mainTitle = state.getTitle().getMainTitle();
        String content = state.getContent();
        String prompt = buildPrompt(mainTitle, content);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        ArticleState.Agent4Result result = parseResult(response);
        List<ArticleState.ImageRequirement> imageRequirements =
                imageRequirementPolicy.apply(result.getImageRequirements());

        state.setContent(StrUtil.blankToDefault(result.getContentWithPlaceholders(), content));
        state.setImageRequirements(imageRequirements);
        log.info("阶段4：配图需求分析完成, taskId={}, count={}", state.getTaskId(), imageRequirements.size());
    }

    /**
     * 构建配图分析提示词。第一阶段只允许 GPT_IMAGE，避免多策略分发提前复杂化。
     */
    private String buildPrompt(String mainTitle, String content) {
        return """
                你是一位专业的新媒体编辑，正在为文章生成配图需求。

                主标题：%s

                正文：
                %s

                可用配图方式：
                %s

                配图方式填写要求：
                %s

                任务要求：
                1. 生成 1 张封面图，position 必须为 1，type 必须为 cover，placeholderId 留空。
                2. 最多再生成 2 张章节配图，position 从 2 开始，type 使用 section。
                3. 章节配图必须在 contentWithPlaceholders 中插入 {{IMAGE_PLACEHOLDER_N}} 占位符，且 placeholderId 必须完全一致。
                4. 所有 imageSource 必须填写 GPT_IMAGE，禁止输出其他配图方式。
                5. prompt 必须使用英文，适合直接交给 gpt-image-2 生成，避免水印、避免侵权角色、避免过多文字。
                6. contentWithPlaceholders 必须保留原正文内容，只添加图片占位符。

                输出格式要求：
                """
                .formatted(mainTitle, content, buildAvailableMethodsDescription(), buildMethodUsageGuide())
                + outputConverter.getFormat();
    }

    /**
     * 返回当前阶段可用的唯一配图方式说明。
     */
    private String buildAvailableMethodsDescription() {
        return "- %s: 使用 OpenAI gpt-image-2 生成原创高质量文章配图".formatted(ImageMethodEnum.GPT_IMAGE.getValue());
    }

    /**
     * 返回 GPT_IMAGE 的字段填写规则。
     */
    private String buildMethodUsageGuide() {
        return """
                - GPT_IMAGE: 在 prompt 字段提供详细英文提示词，描述画面主体、风格、构图、色彩和用途；keywords 可留空。
                """;
    }

    /**
     * 解析模型返回，结构化转换失败时使用本地 JSON 解析兜底。
     */
    private ArticleState.Agent4Result parseResult(String response) {
        try {
            return outputConverter.convert(response);
        }
        catch (RuntimeException e) {
            log.warn("阶段4：结构化配图需求解析失败，尝试使用 JSON 兜底解析", e);
            String jsonText = stripMarkdownFence(response);
            return JSONUtil.toBean(jsonText, ArticleState.Agent4Result.class);
        }
    }

    /**
     * 去除模型偶发返回的 Markdown 代码块包装。
     */
    private String stripMarkdownFence(String response) {
        if (StrUtil.isBlank(response)) {
            throw new IllegalArgumentException("配图需求响应为空");
        }
        return response
                .replaceFirst("^\\s*```json\\s*", "")
                .replaceFirst("^\\s*```\\s*", "")
                .replaceFirst("\\s*```\\s*$", "")
                .trim();
    }
}
