package com.sakura.passage_creator.creation.workflow.image;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 共享速创 GPT-Image-2 图片生成客户端，供 article 和 rednote 复用同一套图片模型调用。
 */
@Component
@Slf4j
public class OpenAiImageGenerationClient {

    /**
     * 速创 GPT-Image-2 异步图片生成接口地址。
     */
    private static final String WUYIN_IMAGE_GENERATION_URL = "https://api.wuyinkeji.com/api/async/image_gpt";

    /**
     * 速创异步任务结果查询接口地址。
     */
    private static final String WUYIN_ASYNC_DETAIL_URL = "https://api.wuyinkeji.com/api/async/detail";

    /**
     * 最多轮询 60 次，避免远程任务卡住时永久占用 workflow 线程。
     */
    private static final int MAX_DETAIL_POLL_ATTEMPTS = 60;

    /**
     * 每 2 秒查询一次结果，兼顾接口 QPS 限制和生成等待时间。
     */
    private static final long DETAIL_POLL_INTERVAL_MILLIS = 3000L;

    /**
     * 图片生成配置，继续复用现有 openai.image 配置前缀。
     */
    private final OpenAiImageProperties properties;

    /**
     * 远程图片下载器，用于处理中转站返回 URL 的场景。
     */
    private final WorkflowRemoteImageDownloader remoteImageDownloader;

    /**
     * Spring REST 客户端。
     */
    private final RestClient restClient;

    public OpenAiImageGenerationClient(OpenAiImageProperties properties, WorkflowRemoteImageDownloader remoteImageDownloader) {
        this.properties = properties;
        this.remoteImageDownloader = remoteImageDownloader;
        this.restClient = RestClient.builder().build();
    }

    /**
     * 根据提示词生成图片，返回统一的 workflow 图片数据。
     */
    public WorkflowImageData generateImage(String prompt) {
        validateConfig();
        if (StrUtil.isBlank(prompt)) {
            throw new IllegalArgumentException("图片提示词不能为空");
        }
        Map<String, Object> requestBody = buildRequestBody(prompt);

        log.info("开始调用速创 GPT-Image-2 图片生成, model={}", properties.getModel());
        String responseBody = restClient.post()
                .uri(buildImageGenerationUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", properties.getApiKey())
                .body(requestBody)
                .retrieve()
                .body(String.class);
        String taskId = parseTaskId(responseBody);
        String detailResponseBody = waitForImageDetail(taskId);
        return resolveImageData(detailResponseBody);
    }

    /**
     * 返回当前图片模型名称，用于计费和结果记录。
     */
    public String getModel() {
        return properties.getModel();
    }

    /**
     * 校验速创调用所需配置，避免任务运行到远程调用时才出现模糊错误。
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
     * 构建速创图片生成请求体，仅传递文档支持的 prompt 和 size。
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("prompt", prompt);
        putIfNotBlank(requestBody, "size", normalizeWuyinSize(properties.getSize()));
        return requestBody;
    }

    /**
     * 构造速创图片生成地址。接口文档要求 key 同时作为查询参数传递。
     */
    private String buildImageGenerationUrl() {
        return buildUrlWithKey(WUYIN_IMAGE_GENERATION_URL);
    }

    /**
     * 构造速创异步结果查询地址。
     */
    private String buildAsyncDetailUrl(String taskId) {
        return UriComponentsBuilder.fromUriString(buildUrlWithKey(WUYIN_ASYNC_DETAIL_URL))
                .queryParam("id", taskId)
                .toUriString();
    }

    /**
     * 按速创文档示例把接口密钥放到 key 查询参数中。
     */
    private String buildUrlWithKey(String url) {
        return UriComponentsBuilder.fromUriString(url)
                .queryParam("key", properties.getApiKey())
                .toUriString();
    }

    /**
     * 从异步提交响应中提取任务 ID。
     */
    private String parseTaskId(String responseBody) {
        JSONObject root = parseResponseRoot(responseBody);
        assertSuccessCode(root, "速创图片生成");

        JSONObject data = root.getJSONObject("data");
        String taskId = data == null ? null : data.getStr("id");
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("速创图片生成响应缺少 data.id: " + responseBody);
        }
        return taskId;
    }

    /**
     * 轮询异步任务详情，直到成功返回图片结果或失败。
     */
    private String waitForImageDetail(String taskId) {
        for (int attempt = 1; attempt <= MAX_DETAIL_POLL_ATTEMPTS; attempt++) {
            String responseBody = restClient.get()
                    .uri(buildAsyncDetailUrl(taskId))
                    .header("Authorization", properties.getApiKey())
                    .retrieve()
                    .body(String.class);
            JSONObject root = parseResponseRoot(responseBody);
            assertSuccessCode(root, "速创图片结果查询");

            JSONObject data = root.getJSONObject("data");
            Integer status = data == null ? null : data.getInt("status");
            if (status != null && status == 2) {
                return responseBody;
            }
            if (status != null && status == 3) {
                String message = data.getStr("message", root.getStr("msg", "未知错误"));
                throw new IllegalStateException("速创图片生成失败: " + message);
            }

            sleepBeforeNextPoll(taskId, attempt);
        }
        throw new IllegalStateException("速创图片生成超时，任务ID: " + taskId);
    }

    /**
     * 把速创详情响应解析成 workflow 图片数据。
     */
    private WorkflowImageData resolveImageData(String detailResponseBody) {
        JSONObject data = parseResponseRoot(detailResponseBody).getJSONObject("data");
        String imageUrl = findFirstImageUrl(data);
        if (StrUtil.isBlank(imageUrl)) {
            throw new IllegalArgumentException("速创图片详情响应缺少图片 URL: " + detailResponseBody);
        }
        return remoteImageDownloader.download(imageUrl);
    }

    /**
     * 递归查找详情数据中的第一条图片 URL，兼容 url、remote_url、images 等常见字段。
     */
    private String findFirstImageUrl(Object value) {
        if (value instanceof CharSequence text) {
            String imageUrl = text.toString();
            return imageUrl.startsWith("http://") || imageUrl.startsWith("https://") ? imageUrl : null;
        }
        if (value instanceof JSONObject jsonObject) {
            for (String key : jsonObject.keySet()) {
                String imageUrl = findFirstImageUrl(jsonObject.get(key));
                if (StrUtil.isNotBlank(imageUrl)) {
                    return imageUrl;
                }
            }
        }
        if (value instanceof JSONArray jsonArray) {
            for (Object item : jsonArray) {
                String imageUrl = findFirstImageUrl(item);
                if (StrUtil.isNotBlank(imageUrl)) {
                    return imageUrl;
                }
            }
        }
        return null;
    }

    /**
     * 解析 JSON 根对象，并在响应为空时给出清晰错误。
     */
    private JSONObject parseResponseRoot(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            throw new IllegalArgumentException("速创图片接口响应为空");
        }
        return JSONUtil.parseObj(responseBody);
    }

    /**
     * 校验速创业务状态码，避免把失败响应继续当作图片结果处理。
     */
    private void assertSuccessCode(JSONObject root, String operation) {
        Integer code = root.getInt("code");
        if (code != null && code != 200) {
            throw new IllegalStateException(operation + "失败: " + root.getStr("msg", root.toString()));
        }
    }

    /**
     * 等待下一次轮询，并在中断时恢复线程中断标记。
     */
    private void sleepBeforeNextPoll(String taskId, int attempt) {
        try {
            Thread.sleep(DETAIL_POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待速创图片生成结果时被中断，任务ID: " + taskId + ", attempt=" + attempt, e);
        }
    }

    /**
     * 将旧 OpenAI 尺寸配置转换成速创文档要求的比例值。
     */
    private String normalizeWuyinSize(String size) {
        if (StrUtil.isBlank(size)) {
            return null;
        }
        return switch (size.trim()) {
            case "1024x1024" -> "1:1";
            case "1536x1024" -> "3:2";
            case "1024x1536" -> "2:3";
            case "1792x1024", "1536x864" -> "16:9";
            case "1024x1792", "864x1536" -> "9:16";
            default -> size;
        };
    }

    /**
     * 只在配置有值时传递可选参数，增强对中转站兼容层的容错。
     */
    private void putIfNotBlank(Map<String, Object> requestBody, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            requestBody.put(key, value);
        }
    }
}
