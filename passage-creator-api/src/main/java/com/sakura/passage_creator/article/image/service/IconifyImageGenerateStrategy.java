package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.config.IconifyImageProperties;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Iconify 图标策略，适合观点、标签、工具列表等轻量视觉点缀。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IconifyImageGenerateStrategy implements ImageGenerateStrategy {

    private final IconifyImageProperties properties;

    private final RemoteImageDownloader remoteImageDownloader;

    private final RestClient restClient = RestClient.builder().build();

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.ICONIFY;
    }

    /**
     * 搜索第一枚匹配图标，并下载 SVG 源文件上传到 OSS。
     */
    @Override
    public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
        String keywords = ImageRequirementTextResolver.resolve(requirement, getMethod());
        if (StrUtil.isBlank(keywords)) {
            throw new IllegalArgumentException("Iconify 配图关键词为空");
        }

        String responseBody = restClient.get()
                .uri(buildSearchUrl(keywords))
                .retrieve()
                .body(String.class);
        String iconName = extractFirstIconName(responseBody);
        String svgUrl = buildSvgUrl(iconName);
        log.info("Iconify 图标检索成功, keywords={}, icon={}", keywords, iconName);
        return ImageGenerationResult.builder()
                .method(getMethod())
                .imageData(remoteImageDownloader.download(svgUrl))
                .description(requirement.getSectionTitle())
                .build();
    }

    /**
     * 构造 Iconify 搜索地址。
     */
    private String buildSearchUrl(String keywords) {
        return UriComponentsBuilder.fromUriString(properties.getApiUrl() + "/search")
                .queryParam("query", keywords)
                .queryParam("limit", properties.getSearchLimit())
                .build()
                .encode()
                .toUriString();
    }

    /**
     * 提取搜索结果中的第一枚图标名称。
     */
    String extractFirstIconName(String responseBody) {
        JSONObject jsonObject = JSONUtil.parseObj(responseBody);
        JSONArray icons = jsonObject.getJSONArray("icons");
        if (icons == null || icons.isEmpty()) {
            throw new IllegalStateException("Iconify 未检索到图标");
        }
        return icons.getStr(0);
    }

    /**
     * 构造 SVG 下载地址，Iconify 使用 prefix/name.svg 形式。
     */
    String buildSvgUrl(String iconName) {
        String iconPath = iconName.replace(":", "/");
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getApiUrl() + "/" + iconPath + ".svg")
                .queryParam("height", properties.getDefaultHeight());
        if (StrUtil.isNotBlank(properties.getDefaultColor())) {
            builder.queryParam("color", properties.getDefaultColor());
        }
        return builder.build().encode().toUriString();
    }
}
