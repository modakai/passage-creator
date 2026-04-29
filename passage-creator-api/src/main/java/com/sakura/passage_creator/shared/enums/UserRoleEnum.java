package com.sakura.passage_creator.shared.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色枚举。
 *
 * 作者：Sakura
 */
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin"),
    BAN("封禁", "ban");

    /**
     * 展示文案。
     */
    private final String text;

    /**
     * 角色值。
     */
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取全部角色值。
     *
     * @return 角色值列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据值获取枚举。
     *
     * @param value 角色值
     * @return 枚举对象
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum item : UserRoleEnum.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
