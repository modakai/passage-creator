package com.sakura.passage_creator.shared.enums;

import lombok.Getter;

/**
 * 审计操作类型枚举，放在共享模块中供审计注解和业务模块共同引用。
 *
 * @author Sakura
 */
@Getter
public enum AuditOperationTypeEnum {

    /**
     * 新增。
     */
    CREATE("create"),

    /**
     * 修改。
     */
    UPDATE("update"),

    /**
     * 删除。
     */
    DELETE("delete"),

    /**
     * 查询。
     */
    QUERY("query"),

    /**
     * 导出。
     */
    EXPORT("export"),

    /**
     * 其他。
     */
    OTHER("other");

    /**
     * 持久化操作类型编码。
     */
    private final String value;

    AuditOperationTypeEnum(String value) {
        this.value = value;
    }
}
