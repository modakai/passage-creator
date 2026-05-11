package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.config.PexelsImageProperties;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Pexels 图库检索策略，适合场景、人物、实物类文章配图。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PexelsImageGenerateStrategy implements ImageGenerateStrategy {

    private final PexelsImageProperties properties;

    private final RemoteImageDownloader remoteImageDownloader;

    private final RestClient restClient = RestClient.builder().build();

    @Override
    public ImageMethodEnum getMethod() {
        return ImageMethodEnum.PEXELS;
    }

    @Override
    public boolean isAvailable() {
        return StrUtil.isNotBlank(properties.getApiKey());
    }

    /**
     * 使用关键词搜索 Pexels，并把命中的远程图片下载为 ImageData 交给 OSS 上传。
     */
    @Override
    public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
        String keywords = ImageRequirementTextResolver.resolve(requirement, getMethod());
        if (StrUtil.isBlank(keywords)) {
            throw new IllegalArgumentException("Pexels 配图关键词为空");
        }

        String responseBody = restClient.get()
                .uri(buildSearchUrl(keywords))
                .header("Authorization", properties.getApiKey())
                .retrieve()
                .body(String.class);
        String imageUrl = extractFirstPhotoUrl(responseBody);
        log.info("Pexels 图片检索成功, keywords={}, url={}", keywords, imageUrl);
        return ImageGenerationResult.builder()
                .method(getMethod())
                .imageData(remoteImageDownloader.download(imageUrl))
                .build();
    }

    /**
     * 构造搜索地址，并交给 UriComponentsBuilder 处理关键词编码。
     */
    private String buildSearchUrl(String keywords) {
        return UriComponentsBuilder.fromUriString(properties.getApiUrl())
                .queryParam("query", keywords)
                .queryParam("per_page", properties.getPerPage())
                .queryParam("orientation", properties.getOrientation())
                .build()
                .encode()
                .toUriString();
    }

    /**
     * 从 Pexels 响应中取第一张图，优先使用 large2x，缺失时降级到 large/original。
     */
    String extractFirstPhotoUrl(String responseBody) {
        JSONObject jsonObject = JSONUtil.parseObj(responseBody);
        JSONArray photos = jsonObject.getJSONArray("photos");
        if (photos == null || photos.isEmpty()) {
            throw new IllegalStateException("Pexels 未检索到图片");
        }
        JSONObject photo = photos.getJSONObject(0);
        JSONObject src = photo.getJSONObject("src");
        String imageUrl = firstNotBlank(src.getStr("large2x"), src.getStr("large"), src.getStr("original"));
        if (StrUtil.isBlank(imageUrl)) {
            throw new IllegalStateException("Pexels 响应缺少图片 URL");
        }
        return imageUrl;
    }

    /**
     * 返回第一个非空字符串，避免不同 Pexels 字段缺失时直接失败。
     */
    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
