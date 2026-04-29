package com.sakura.passage_creator.shared.context;

/**
 * 当前请求登录用户上下文。
 */
public final class LoginUserContext {

    /**
     * 当前线程绑定的登录用户快照，避免共享模块依赖用户实体。
     */
    private static final ThreadLocal<LoginUserInfo> LOGIN_USER_HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    /**
     * 保存当前请求的登录用户。
     *
     * @param user 登录用户快照
     */
    public static void setLoginUser(LoginUserInfo user) {
        LOGIN_USER_HOLDER.set(user);
    }

    /**
     * 获取当前请求的登录用户。
     *
     * @return 登录用户快照
     */
    public static LoginUserInfo getLoginUser() {
        return LOGIN_USER_HOLDER.get();
    }

    /**
     * 清理当前线程中的登录用户，避免线程复用导致数据串扰。
     */
    public static void clear() {
        LOGIN_USER_HOLDER.remove();
    }
}
