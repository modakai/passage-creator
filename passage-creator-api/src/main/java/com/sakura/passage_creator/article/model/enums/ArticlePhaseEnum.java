package com.sakura.passage_creator.article.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 文章阶段枚举
 *
 * @author sakura
 * @create 2026-04
 */
@Getter
public enum ArticlePhaseEnum {
    // PENDING              等待处理
    //TITLE_GENERATING     标题生成中
    //TITLE_SELECTING      等待用户选择标题
    //OUTLINE_GENERATING   大纲生成中
    //OUTLINE_EDITING      等待用户编辑大纲
    //CONTENT_GENERATING   正文生成中
    //COMPLETED            已完成
    //FAILED               失败
    PENDING("PENDING", "等待处理"),
    TITLE_GENERATING("TITLE_GENERATING", "生成标题中"),
    TITLE_SELECTING("TITLE_SELECTING", "等待选择标题"),
    OUTLINE_GENERATING("OUTLINE_GENERATING", "生成大纲中"),
    OUTLINE_EDITING("OUTLINE_EDITING", "等待编辑大纲"),
    CONTENT_GENERATING("CONTENT_GENERATING", "生成正文中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败");

    final String value;
    final String description;

    ArticlePhaseEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 获取下一个状态
     * COMPLETED 和 FAILED 自行选择
     *
     * @return 对应状态的值
     */
    public String next() {
        ArticlePhaseEnum phaseEnum = switch (this) {
            case PENDING -> TITLE_GENERATING;
            case TITLE_GENERATING -> TITLE_SELECTING;
            case TITLE_SELECTING -> OUTLINE_GENERATING;
            case OUTLINE_GENERATING -> OUTLINE_EDITING;
            case OUTLINE_EDITING -> CONTENT_GENERATING;
            case CONTENT_GENERATING -> COMPLETED;
            default -> FAILED;
        };
        return phaseEnum.getValue();

    }

    /**
     * 根据值获取枚举。
     *
     * @param value 阶段值
     * @return 枚举对象
     */
    public static ArticlePhaseEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (ArticlePhaseEnum item : ArticlePhaseEnum.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
