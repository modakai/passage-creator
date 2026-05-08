package com.sakura.passage_creator.prompt.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 模板渲染器，负责变量声明校验和占位符替换。
 */
@Component
public class PromptTemplateRenderer {

    /**
     * 模板占位符匹配表达式，仅支持 {name} 这类显式变量。
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)}");

    /**
     * 渲染 Prompt 模板内容。
     *
     * @param template Prompt 模板文本
     * @param variableSchema 变量定义 JSON
     * @param variables 实际变量值
     * @return 渲染后的 Prompt 文本
     */
    public String render(String template, String variableSchema, Map<String, ?> variables) {
        if (StringUtils.isBlank(template)) {
            return template;
        }
        VariableDefinition definition = parseVariableDefinition(variableSchema);
        validateRequiredVariables(definition.requiredVariables(), variables);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables == null ? null : variables.get(variableName);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * 校验模板中的变量是否均已在变量定义中声明。
     *
     * @param template Prompt 模板文本
     * @param variableSchema 变量定义 JSON
     */
    public void validateTemplateVariables(String template, String variableSchema) {
        VariableDefinition definition = parseVariableDefinition(variableSchema);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(StringUtils.defaultString(template));
        while (matcher.find()) {
            if (!definition.allVariables().contains(matcher.group(1))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Prompt 模板存在未声明变量：" + matcher.group(1));
            }
        }
    }

    /**
     * 解析变量定义 JSON。
     *
     * @param variableSchema 变量定义 JSON
     * @return 变量定义
     */
    private VariableDefinition parseVariableDefinition(String variableSchema) {
        Set<String> allVariables = new HashSet<>();
        Set<String> requiredVariables = new HashSet<>();
        if (StringUtils.isBlank(variableSchema)) {
            return new VariableDefinition(allVariables, requiredVariables);
        }
        if (!JSONUtil.isTypeJSONArray(variableSchema)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Prompt 变量定义必须是 JSON 数组");
        }
        JSONArray array = JSONUtil.parseArray(variableSchema);
        for (int i = 0; i < array.size(); i++) {
            String name = array.getJSONObject(i).getStr("name");
            if (StringUtils.isBlank(name)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Prompt 变量名不能为空");
            }
            allVariables.add(name);
            if (Boolean.TRUE.equals(array.getJSONObject(i).getBool("required"))) {
                requiredVariables.add(name);
            }
        }
        return new VariableDefinition(allVariables, requiredVariables);
    }

    /**
     * 校验必填变量是否传入了非空值。
     *
     * @param requiredVariables 必填变量名集合
     * @param variables 实际变量值
     */
    private void validateRequiredVariables(Set<String> requiredVariables, Map<String, ?> variables) {
        for (String variableName : requiredVariables) {
            Object value = variables == null ? null : variables.get(variableName);
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Prompt 缺少必填变量：" + variableName);
            }
        }
    }

    /**
     * Prompt 模板变量定义。
     *
     * @param allVariables 全部声明变量
     * @param requiredVariables 必填变量
     */
    private record VariableDefinition(Set<String> allVariables, Set<String> requiredVariables) {
    }
}
