package com.sakura.passage_creator.prompt.model.enums;

import java.util.Arrays;

/**
 * Prompt 模板状态枚举。
 */
public enum PromptTemplateStatusEnum {

    /**
     * 草稿状态，允许编辑但不会被运行时读取。
     */
    DRAFT("DRAFT"),

    /**
     * 生效状态，同一个模板标识和环境只允许一个生效版本。
     */
    ACTIVE("ACTIVE"),

    /**
     * 归档状态，保留历史但不再被运行时读取。
     */
    ARCHIVED("ARCHIVED");

    /**
     * 数据库存储值。
     */
    private final String value;

    PromptTemplateStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 判断状态值是否属于受控集合。
     *
     * @param value 状态值
     * @return 是否合法
     */
    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(item -> item.value.equals(value));
    }
}
