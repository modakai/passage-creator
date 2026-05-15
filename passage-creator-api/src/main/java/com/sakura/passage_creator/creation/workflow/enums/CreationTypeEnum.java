package com.sakura.passage_creator.creation.workflow.enums;

import lombok.Getter;

/**
 * 创作类型枚举，持久化值使用稳定的小写机器标识。
 */
@Getter
public enum CreationTypeEnum {

    /**
     * 文章创作。
     */
    ARTICLE("article"),

    /**
     * 小红书风格创作预留类型，代码和数据库统一使用 rednote。
     * https://my.feishu.cn/wiki/Vtn4w4xndisR1zknE37ccLNunDf
     */
    REDNOTE("rednote"),

    /**
     * 短视频脚本创作预留类型。
     */
    VIDEO_SCRIPT("video_script"),

    /**
     * 营销文案创作预留类型。
     */
    MARKETING_COPY("marketing_copy");

    private final String value;

    CreationTypeEnum(String value) {
        this.value = value;
    }
}
