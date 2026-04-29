package com.sakura.passage_creator.shared.annotation;

import com.sakura.passage_creator.shared.enums.AuditOperationTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理员重要操作审计注解。
 *
 * @author Sakura
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLogRecord {

    /**
     * 操作描述，例如“删除用户”。
     */
    String description();

    /**
     * 业务模块。
     */
    String module() default "";

    /**
     * 操作类型。
     */
    AuditOperationTypeEnum operationType() default AuditOperationTypeEnum.OTHER;

    /**
     * 是否记录请求摘要。
     */
    boolean recordRequest() default true;

    /**
     * 是否记录响应摘要。
     */
    boolean recordResponse() default false;
}
