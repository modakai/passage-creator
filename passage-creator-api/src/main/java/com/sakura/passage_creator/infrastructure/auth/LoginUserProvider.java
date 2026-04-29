package com.sakura.passage_creator.infrastructure.auth;

import com.sakura.passage_creator.shared.context.LoginUserInfo;

/**
 * 登录用户加载端口，由用户模块提供具体实现，避免基础设施直接依赖用户内部服务。
 */
public interface LoginUserProvider {

    /**
     * 根据用户 id 加载当前登录用户快照。
     *
     * @param userId 用户 id
     * @return 登录用户快照，用户不存在时返回 null
     */
    LoginUserInfo loadLoginUser(Long userId);
}
