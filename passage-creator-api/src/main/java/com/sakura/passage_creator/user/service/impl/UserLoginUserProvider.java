package com.sakura.passage_creator.user.service.impl;

import com.sakura.passage_creator.infrastructure.auth.LoginUserProvider;
import com.sakura.passage_creator.infrastructure.auth.LoginUserCache;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.enums.UserRoleEnum;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.user.model.entity.User;
import com.sakura.passage_creator.user.repository.UserMapper;
import org.springframework.stereotype.Component;

/**
 * 用户模块提供给认证基础设施使用的登录用户快照加载器。
 */
@Component
public class UserLoginUserProvider implements LoginUserProvider {

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * 登录用户快照缓存。
     */
    private final LoginUserCache loginUserCache;

    public UserLoginUserProvider(UserMapper userMapper, LoginUserCache loginUserCache) {
        this.userMapper = userMapper;
        this.loginUserCache = loginUserCache;
    }

    /**
     * 根据用户 id 查询用户并校验登录态可用性。
     *
     * @param userId 用户 id
     * @return 登录用户快照
     */
    @Override
    public LoginUserInfo loadLoginUser(Long userId) {
        if (userId == null) {
            return null;
        }
        LoginUserInfo cachedLoginUser = loginUserCache.get(userId);
        if (cachedLoginUser != null) {
            return cachedLoginUser;
        }
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            return null;
        }
        validateUserLoginStatus(user);
        LoginUserInfo loginUserInfo = new LoginUserInfo(user.getId(), user.getUserAccount(), user.getUserName(),
                user.getUserRole());
        loginUserCache.put(loginUserInfo);
        return loginUserInfo;
    }

    /**
     * 校验用户是否允许继续访问系统。
     *
     * @param user 用户实体
     */
    private void validateUserLoginStatus(User user) {
        if (UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "auth.user.banned");
        }
        if (UserConstant.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "auth.user.disabled");
        }
    }
}
