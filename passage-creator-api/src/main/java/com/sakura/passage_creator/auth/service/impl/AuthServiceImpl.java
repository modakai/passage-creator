package com.sakura.passage_creator.auth.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.audit.api.AuditApi;
import com.sakura.passage_creator.audit.api.LoginAuditCommand;
import com.sakura.passage_creator.auth.model.vo.LoginUserVO;
import com.sakura.passage_creator.auth.service.AuthService;
import com.sakura.passage_creator.auth.service.OnlineUserService;
import com.sakura.passage_creator.infrastructure.auth.TokenManager;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.context.LoginUserContext;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.enums.UserRoleEnum;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.util.NetUtils;
import com.sakura.passage_creator.user.model.entity.User;
import com.sakura.passage_creator.user.repository.UserMapper;
import io.github.linpeilie.Converter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import static com.sakura.passage_creator.user.model.entity.table.UserTableDef.USER;

/**
 * 认证服务实现。
 *
 * 作者：Sakura
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    /**
     * 用户数据访问对象。
     */
    private final UserMapper userMapper;

    /**
     * Token 管理器。
     */
    private final TokenManager tokenManager;

    /**
     * 审计模块 API。
     */
    private final AuditApi auditApi;

    /**
     * 在线用户服务。
     */
    private final OnlineUserService onlineUserService;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    private final Converter converter;

    public AuthServiceImpl(UserMapper userMapper, TokenManager tokenManager, Converter converter) {
        this(userMapper, tokenManager, converter, null, null);
    }

    @Autowired
    public AuthServiceImpl(UserMapper userMapper, TokenManager tokenManager, Converter converter, AuditApi auditApi,
            OnlineUserService onlineUserService) {
        this.userMapper = userMapper;
        this.tokenManager = tokenManager;
        this.converter = converter;
        this.auditApi = auditApi;
        this.onlineUserService = onlineUserService;
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.param.blank");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.user_account.too_short");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.user_password.too_short");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.password.not_match");
        }

        synchronized (userAccount.intern()) {
            QueryWrapper queryWrapper = QueryWrapper.create().where(USER.USER_ACCOUNT.eq(userAccount));
            long count = userMapper.selectCountByQuery(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.account.duplicate");
            }

            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword(userPassword));
            // 注册接口只允许创建普通用户（user），角色不允许由客户端或其他参数决定。
            user.setUserRole(UserRoleEnum.USER.getValue());
            user.setStatus(UserConstant.STATUS_ENABLED);
            int saveResult = userMapper.insertSelective(user);
            if (saveResult <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "auth.register.db_error");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        User loginUser = null;
        try {
            if (StringUtils.isAnyBlank(userAccount, userPassword)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.param.blank");
            }
            if (userAccount.length() < 4) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.account.invalid");
            }
            if (userPassword.length() < 8) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.password.invalid");
            }

            QueryWrapper queryWrapper = QueryWrapper.create()
                    .where(USER.USER_ACCOUNT.eq(userAccount))
                    .and(USER.USER_PASSWORD.eq(encryptPassword(userPassword)));
            User user = userMapper.selectOneByQuery(queryWrapper);
            if (user == null) {
                log.info("user login failed, userAccount cannot match userPassword");
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "auth.login.invalid");
            }
            validateUserLoginStatus(user);
            loginUser = user;
            LoginUserVO loginUserVO = buildLoginUserVOWithToken(user, request);
            recordLoginAudit(userAccount, loginUser, request, true, null, startTime);
            return loginUserVO;
        } catch (BusinessException e) {
            recordLoginAudit(userAccount, loginUser, request, false, e.getMessage(), startTime);
            throw e;
        } catch (RuntimeException e) {
            recordLoginAudit(userAccount, loginUser, request, false, e.getClass().getSimpleName(), startTime);
            throw e;
        }
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        synchronized (unionId.intern()) {
            QueryWrapper queryWrapper = QueryWrapper.create().where(USER.UNION_ID.eq(unionId));
            User user = userMapper.selectOneByQuery(queryWrapper);
            if (user != null) {
                validateUserLoginStatus(user);
            }
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                // 第三方登录首次创建用户时同样强制写入普通用户角色，避免角色为空带来权限歧义。
                user.setUserRole(UserRoleEnum.USER.getValue());
                user.setStatus(UserConstant.STATUS_ENABLED);
                int result = userMapper.insert(user);
                if (result <= 0) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "auth.login.fail");
                }
            }
            return buildLoginUserVOWithToken(user, request);
        }
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        LoginUserInfo currentUser = LoginUserContext.getLoginUser();
        if (currentUser == null || currentUser.userId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User user = userMapper.selectOneById(currentUser.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        validateUserLoginStatus(user);
        return user;
    }

    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        LoginUserInfo currentUser = LoginUserContext.getLoginUser();
        if (currentUser == null || currentUser.userId() == null) {
            return null;
        }
        User user = userMapper.selectOneById(currentUser.userId());
        if (user == null) {
            return null;
        }
        validateUserLoginStatus(user);
        return user;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        LoginUserInfo currentUser = LoginUserContext.getLoginUser();
        return currentUser != null && UserRoleEnum.ADMIN.getValue().equals(currentUser.userRole());
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        String token = tokenManager.resolveToken(request);
        if (tokenManager.getUserId(token) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "auth.logout.not_login");
        }
        tokenManager.removeToken(token);
        if (onlineUserService != null) {
            onlineUserService.removeByToken(token);
        }
        LoginUserContext.clear();
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        // 登录用户视图手写映射，避免 MapStruct Plus 在 user 模块生成依赖 auth 模块的 mapper。
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserProfile(user.getUserProfile());
        loginUserVO.setUserRole(user.getUserRole());
        loginUserVO.setStatus(user.getStatus());
        loginUserVO.setCreateTime(user.getCreateTime());
        loginUserVO.setUpdateTime(user.getUpdateTime());
        return loginUserVO;
    }

    /**
     * 加密用户密码。
     *
     * @param userPassword 明文密码
     * @return 密文密码
     */
    private String encryptPassword(String userPassword) {
        return DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + userPassword).getBytes());
    }

    /**
     * 构建带 token 的登录用户视图。
     *
     * @param user 用户实体
     * @return 登录用户视图
     */
    private LoginUserVO buildLoginUserVOWithToken(User user, HttpServletRequest request) {
        String oldToken = tokenManager.getTokenByUserId(user.getId());
        if (onlineUserService != null) {
            onlineUserService.removeByToken(oldToken);
        }
        String token = tokenManager.generateToken();
        tokenManager.storeToken(user.getId(), token);
        if (onlineUserService != null) {
            onlineUserService.recordLoginSession(user.getId(), user.getUserAccount(), user.getUserName(),
                    user.getUserRole(), token, request);
        }
        LoginUserVO loginUserVO = getLoginUserVO(user);
        loginUserVO.setToken(token);
        return loginUserVO;
    }

    /**
     * 记录登录审计日志，审计失败不能影响登录主流程。
     *
     * @param userAccount 登录账号
     * @param user 用户实体
     * @param request HTTP 请求
     * @param success 是否登录成功
     * @param failureReason 失败原因
     * @param startTime 开始时间
     */
    private void recordLoginAudit(String userAccount, User user, HttpServletRequest request,
            boolean success, String failureReason, long startTime) {
        if (auditApi == null) {
            return;
        }
        try {
            LoginAuditCommand command = new LoginAuditCommand(
                    user == null ? null : user.getId(),
                    userAccount,
                    request == null ? null : NetUtils.getIpAddress(request),
                    request == null ? null : request.getHeader("User-Agent"),
                    success,
                    failureReason,
                    System.currentTimeMillis() - startTime
            );
            auditApi.submitLoginLog(command);
        } catch (Exception e) {
            log.error("record login audit failed", e);
        }
    }

    /**
     * 校验用户是否允许登录或继续访问。
     *
     * @param user 用户实体
     */
    private void validateUserLoginStatus(User user) {
        if (user == null) {
            return;
        }
        if (UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "auth.user.banned");
        }
        if (UserConstant.STATUS_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "auth.user.disabled");
        }
    }
}
