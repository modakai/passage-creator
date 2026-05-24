package com.sakura.passage_creator.shared.constant;

/**
 * 用户常量
 *
 * @author Sakura
 */
public interface UserConstant {

    // region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 内置超级管理员账号，不允许删除。
     */
    String PROTECTED_SUPER_ADMIN_ACCOUNT = "sakura";

    /**
     * 封禁角色
     */
    String BAN_ROLE = "ban";

    /**
     * 用户启用状态。
     */
    Integer STATUS_ENABLED = 1;

    /**
     * 用户禁用状态。
     */
    Integer STATUS_DISABLED = 0;

    // endregion
}
