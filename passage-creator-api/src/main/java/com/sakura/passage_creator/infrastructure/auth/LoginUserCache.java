package com.sakura.passage_creator.infrastructure.auth;

import com.sakura.passage_creator.shared.context.LoginUserInfo;

/**
 * 登录用户快照缓存端口。
 */
public interface LoginUserCache {

    /**
     * 根据用户 id 获取登录用户快照。
     *
     * @param userId 用户 id
     * @return 登录用户快照，缓存不存在时返回 null
     */
    LoginUserInfo get(Long userId);

    /**
     * 写入登录用户快照缓存。
     *
     * @param loginUserInfo 登录用户快照
     */
    void put(LoginUserInfo loginUserInfo);

    /**
     * 删除指定用户的登录用户快照缓存。
     *
     * @param userId 用户 id
     */
    void evict(Long userId);
}
