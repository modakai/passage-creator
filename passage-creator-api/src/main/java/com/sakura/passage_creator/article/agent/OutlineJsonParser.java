package com.sakura.passage_creator.article.agent;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.article.agent.state.ArticleState;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 大纲模型输出解析器。
 * <p>
 * 模型偶尔会返回 Markdown 包裹或漏逗号的 JSON，这里只修复可确定的格式问题，
 * 结构不合法时继续失败，避免把坏大纲传给正文生成阶段。
 */
final class OutlineJsonParser {

    /**
     * 对象属性之间漏逗号的常见模式，例如 "section": 1 换行后直接接 "title"。
     */
    private static final Pattern MISSING_COMMA_BEFORE_OBJECT_KEY = Pattern.compile(
            "(\\]|\\}|\"|-?\\d+(?:\\.\\d+)?|true|false|null)(\\s*\\R\\s*)(\"(?:sections|section|title|points)\"\\s*:)"
    );

    /**
     * 数组字符串元素之间漏逗号的常见模式，例如 ["要点1" "要点2"]。
     */
    private static final Pattern MISSING_COMMA_BETWEEN_STRING_VALUES = Pattern.compile(
            "(\"(?:[^\"\\\\]|\\\\.)*\")(\\s+)(\"(?:[^\"\\\\]|\\\\.)*\")"
    );

    /**
     * 字符串字段值缺少开引号的常见模式，例如 "title": 引言内容"。
     */
    private static final Pattern MISSING_OPENING_QUOTE_FOR_STRING_VALUE = Pattern.compile(
            "(\"(?:title)\"\\s*:\\s*)(?!\\s*\")([^,\\r\\n{}\\[\\]]*?\")(\\s*[,\\r\\n])"
    );

    private OutlineJsonParser() {
    }

    /**
     * 将模型原始输出解析成大纲对象。
     *
     * @param modelOutput 模型原始输出
     * @return 结构合法的大纲结果
     */
    static ArticleState.OutlineResult parse(String modelOutput) {
        String normalizedOutput = repairCommonJsonMistakes(modelOutput);
        String jsonText = repairCommonJsonMistakes(extractFirstJsonObject(normalizedOutput));
        ArticleState.OutlineResult outline;
        try {
            outline = JSONUtil.toBean(jsonText, new TypeReference<ArticleState.OutlineResult>() {
            }, true);
        }
        catch (RuntimeException e) {
            throw new IllegalArgumentException("大纲 JSON 解析失败，请检查模型输出是否为合法 JSON", e);
        }

        validateOutline(outline);
        return outline;
    }

    /**
     * 从模型输出中提取第一个完整 JSON 对象，兼容 ```json 代码块和前后解释文本。
     */
    private static String extractFirstJsonObject(String modelOutput) {
        if (StrUtil.isBlank(modelOutput)) {
            throw new IllegalArgumentException("大纲 JSON 内容为空");
        }

        int start = modelOutput.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("大纲 JSON 内容缺少对象起始符");
        }

        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int i = start; i < modelOutput.length(); i++) {
            char current = modelOutput.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = inString;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            }
            if (current == '}') {
                depth--;
                if (depth == 0) {
                    return modelOutput.substring(start, i + 1);
                }
            }
        }

        throw new IllegalArgumentException("大纲 JSON 内容缺少对象结束符");
    }

    /**
     * 修复模型最常见的 JSON 笔误，只处理确定性高的漏逗号场景。
     */
    private static String repairCommonJsonMistakes(String jsonText) {
        String repaired = jsonText.trim();
        repaired = MISSING_OPENING_QUOTE_FOR_STRING_VALUE.matcher(repaired).replaceAll("$1\"$2$3");
        repaired = MISSING_COMMA_BEFORE_OBJECT_KEY.matcher(repaired).replaceAll("$1,$2$3");
        repaired = MISSING_COMMA_BETWEEN_STRING_VALUES.matcher(repaired).replaceAll("$1,$2$3");
        return repaired;
    }

    /**
     * 校验大纲业务结构，防止空章节、空标题、空要点继续流入后续阶段。
     */
    static void validateOutline(ArticleState.OutlineResult outline) {
        if (outline == null || outline.getSections() == null || outline.getSections().isEmpty()) {
            throw new IllegalArgumentException("大纲 JSON 结构不合法：sections 不能为空");
        }

        List<ArticleState.OutlineSection> sections = outline.getSections();
        for (int i = 0; i < sections.size(); i++) {
            ArticleState.OutlineSection section = sections.get(i);
            if (section == null) {
                throw new IllegalArgumentException("大纲 JSON 结构不合法：章节不能为空");
            }
            if (StrUtil.isBlank(section.getTitle())) {
                throw new IllegalArgumentException("大纲 JSON 结构不合法：章节标题不能为空");
            }
            if (section.getPoints() == null || section.getPoints().stream().allMatch(StrUtil::isBlank)) {
                throw new IllegalArgumentException("大纲 JSON 结构不合法：章节要点不能为空");
            }

            // 重新写入连续章节号，避免模型输出跳号影响前端展示和正文生成。
            section.setSection(i + 1);
            section.setTitle(section.getTitle().trim());
            section.setPoints(section.getPoints().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .toList());
        }
    }
}
