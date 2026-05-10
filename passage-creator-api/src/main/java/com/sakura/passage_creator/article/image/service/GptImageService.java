package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.config.OpenAiImageProperties;
import com.sakura.passage_creator.article.model.dto.image.ImageData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GPT Image 2 图片生成服务，封装 OpenAI Image API 调用。
 */
@Service
@Slf4j
public class GptImageService {

    /**
     * OpenAI 图片配置。
     */
    private final OpenAiImageProperties properties;

    /**
     * OpenAI 图片响应解析器。
     */
    private final OpenAiImageResponseParser responseParser;

    /**
     * 远程图片下载器，用于处理中转站返回 URL 的场景。
     */
    private final RemoteImageDownloader remoteImageDownloader;

    /**
     * Spring REST 客户端。
     */
    private final RestClient restClient;

    public GptImageService(OpenAiImageProperties properties, OpenAiImageResponseParser responseParser,
            RemoteImageDownloader remoteImageDownloader) {
        this.properties = properties;
        this.responseParser = responseParser;
        this.remoteImageDownloader = remoteImageDownloader;
        this.restClient = RestClient.builder().build();
    }

    /**
     * 根据配图需求生成一张图片。
     *
     * @param requirement 配图需求
     * @return 图片二进制数据
     */
    public ImageData generateImage(ArticleState.ImageRequirement requirement) {
        validateConfig();
        String prompt = buildPrompt(requirement);
        Map<String, Object> requestBody = buildRequestBody(prompt);

        log.info("开始调用 OpenAI 图片生成, model={}, position={}", properties.getModel(), requirement.getPosition());
        String responseBody = restClient.post()
                .uri(buildImageGenerationUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(requestBody)
                .retrieve()
                .body(String.class);
        OpenAiImageResponseParser.ParsedImage parsedImage = responseParser.parseFirstImage(responseBody);
        if (parsedImage.hasBase64()) {
            return responseParser.parseImageData(responseBody);
        }
        return remoteImageDownloader.download(parsedImage.url());
    }

    /**
     * 校验 OpenAI 调用所需配置，避免任务运行到远程调用时才出现模糊错误。
     */
    private void validateConfig() {
        if (StrUtil.isBlank(properties.getApiKey())) {
            throw new IllegalStateException("OpenAI API Key 未配置，请设置 OPENAI_API_KEY");
        }
        if (StrUtil.isBlank(properties.getModel())) {
            throw new IllegalStateException("OpenAI 图片模型未配置");
        }
    }

    /**
     * 构建 OpenAI 图片生成请求体。
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("prompt", prompt);
        putIfNotBlank(requestBody, "size", properties.getSize());
        putIfNotBlank(requestBody, "quality", properties.getQuality());
        putIfNotBlank(requestBody, "output_format", properties.getOutputFormat());
        return requestBody;
    }

    /**
     * 构造图片生成地址。中转站通常把 base_url 配成 https://xxx/v1。
     */
    private String buildImageGenerationUrl() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/images/generations";
    }

    /**
     * 只在配置有值时传递可选参数，增强对中转站兼容层的容错。
     */
    private void putIfNotBlank(Map<String, Object> requestBody, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            requestBody.put(key, value);
        }
    }

    /**
     * 优先使用模型分析出的 prompt，缺失时用章节标题和关键词兜底。
     */
    private String buildPrompt(ArticleState.ImageRequirement requirement) {
        if (StrUtil.isNotBlank(requirement.getPrompt())) {
            return requirement.getPrompt();
        }
        String sectionTitle = StrUtil.blankToDefault(requirement.getSectionTitle(), "article illustration");
        String keywords = StrUtil.blankToDefault(requirement.getKeywords(), sectionTitle);
        return "Create a clean editorial illustration for an article section titled \"%s\". Keywords: %s. No watermark."
                .formatted(sectionTitle, keywords);
    }
}
