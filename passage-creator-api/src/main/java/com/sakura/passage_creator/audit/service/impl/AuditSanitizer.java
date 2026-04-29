package com.sakura.passage_creator.audit.service.impl;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 审计摘要脱敏与截断工具。
 *
 * @author Sakura
 */
public class AuditSanitizer {

    /**
     * 默认最大摘要长度。
     */
    private static final int DEFAULT_MAX_LENGTH = 2000;

    /**
     * 敏感字段名称。
     */
    private static final List<String> SENSITIVE_KEYS = List.of(
            "password", "token", "authorization", "captcha", "secret", "key");

    /**
     * 最大摘要长度。
     */
    private final int maxLength;

    public AuditSanitizer() {
        this(DEFAULT_MAX_LENGTH);
    }

    public AuditSanitizer(int maxLength) {
        this.maxLength = Math.max(4, maxLength);
    }

    /**
     * 脱敏并截断审计摘要。
     *
     * @param raw 原始摘要
     * @return 安全摘要
     */
    public String sanitize(String raw) {
        if (StringUtils.isBlank(raw)) {
            return raw;
        }
        String sanitized = raw;
        for (String key : SENSITIVE_KEYS) {
            sanitized = maskJsonStringValue(sanitized, key);
            sanitized = maskKeyValuePair(sanitized, key);
        }
        return truncate(sanitized);
    }

    /**
     * 屏蔽 JSON 字符串中的敏感字段值。
     */
    private String maskJsonStringValue(String value, String key) {
        Pattern pattern = Pattern.compile("(?i)(\"" + Pattern.quote(key) + "\"\\s*:\\s*\")([^\"]*)(\")");
        return pattern.matcher(value).replaceAll("$1***$3");
    }

    /**
     * 屏蔽普通 key=value 参数中的敏感字段值。
     */
    private String maskKeyValuePair(String value, String key) {
        Pattern pattern = Pattern.compile("(?i)(\\b" + Pattern.quote(key) + "\\s*=\\s*)([^,\\s&}]+)");
        return pattern.matcher(value).replaceAll("$1***");
    }

    /**
     * 对摘要做长度保护。
     */
    private String truncate(String value) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
