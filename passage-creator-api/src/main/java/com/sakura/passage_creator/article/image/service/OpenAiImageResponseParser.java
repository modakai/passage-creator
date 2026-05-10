package com.sakura.passage_creator.article.image.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.article.model.dto.image.ImageData;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * OpenAI 图片响应解析器，兼容官方 b64_json 和中转站 url 两种返回格式。
 */
@Component
public class OpenAiImageResponseParser {

    private static final String DEFAULT_MIME_TYPE = "image/png";

    private static final String DEFAULT_EXTENSION = ".png";

    /**
     * 解析 OpenAI Image API 返回体中的第一张图片。
     *
     * @param responseBody OpenAI 原始 JSON 响应
     * @return 图片二进制数据
     */
    public ImageData parseImageData(String responseBody) {
        ParsedImage parsedImage = parseFirstImage(responseBody);
        if (!parsedImage.hasBase64()) {
            throw new IllegalArgumentException("OpenAI 图片响应缺少 b64_json");
        }
        String normalizedBase64 = normalizeBase64(parsedImage.b64Json());
        return ImageData.builder()
                .bytes(Base64.getDecoder().decode(normalizedBase64))
                .mimeType(DEFAULT_MIME_TYPE)
                .extension(DEFAULT_EXTENSION)
                .build();
    }

    /**
     * 解析第一张图片的原始载荷。兼容中转站常见的 url 返回。
     *
     * @param responseBody OpenAI 或兼容服务的原始 JSON 响应
     * @return 第一张图片载荷
     */
    public ParsedImage parseFirstImage(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            throw new IllegalArgumentException("OpenAI 图片响应为空");
        }

        JSONArray dataArray = JSONUtil.parseObj(responseBody).getJSONArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            throw new IllegalArgumentException("OpenAI 图片响应缺少 data");
        }

        String base64Image = dataArray.getJSONObject(0).getStr("b64_json");
        String imageUrl = dataArray.getJSONObject(0).getStr("url");
        if (StrUtil.isBlank(base64Image) && StrUtil.isBlank(imageUrl)) {
            throw new IllegalArgumentException("OpenAI 图片响应缺少图片数据");
        }

        return new ParsedImage(base64Image, imageUrl);
    }

    /**
     * 兼容中转站返回 data URL 或省略 padding 的 base64 图片数据。
     */
    private String normalizeBase64(String base64Image) {
        String value = base64Image.trim();
        if (value.startsWith("data:")) {
            int commaIndex = value.indexOf(',');
            if (commaIndex < 0 || commaIndex == value.length() - 1) {
                throw new IllegalArgumentException("OpenAI 图片 data URL 格式不合法");
            }
            value = value.substring(commaIndex + 1);
        }
        int paddingLength = (4 - value.length() % 4) % 4;
        return value + "=".repeat(paddingLength);
    }

    /**
     * OpenAI 兼容图片响应中的第一张图片数据。
     */
    public record ParsedImage(String b64Json, String url) {

        /**
         * 是否包含 base64 图片数据。
         */
        public boolean hasBase64() {
            return StrUtil.isNotBlank(b64Json);
        }

        /**
         * 是否包含可下载的图片 URL。
         */
        public boolean hasUrl() {
            return StrUtil.isNotBlank(url);
        }
    }
}
