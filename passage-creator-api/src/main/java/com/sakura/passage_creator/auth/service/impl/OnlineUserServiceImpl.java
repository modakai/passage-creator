package com.sakura.passage_creator.auth.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.sakura.passage_creator.auth.model.dto.OnlineUserQueryRequest;
import com.sakura.passage_creator.auth.model.entity.OnlineUserSession;
import com.sakura.passage_creator.auth.model.vo.OnlineUserVO;
import com.sakura.passage_creator.auth.repository.OnlineUserSessionRepository;
import com.sakura.passage_creator.auth.service.OnlineUserService;
import com.sakura.passage_creator.infrastructure.auth.LoginUserProvider;
import com.sakura.passage_creator.infrastructure.auth.TokenManager;
import com.sakura.passage_creator.infrastructure.auth.TokenProperties;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.context.LoginUserInfo;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.util.NetUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线用户服务实现。
 *
 * @author Sakura
 */
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    /**
     * 在线会话仓储。
     */
    private final OnlineUserSessionRepository sessionRepository;

    /**
     * Token 管理器。
     */
    private final TokenManager tokenManager;

    /**
     * Token 配置。
     */
    private final TokenProperties tokenProperties;

    /**
     * 登录用户快照加载器，用于复用 login:user-info:* 缓存。
     */
    private final LoginUserProvider loginUserProvider;

    public OnlineUserServiceImpl(OnlineUserSessionRepository sessionRepository, TokenManager tokenManager) {
        this(sessionRepository, tokenManager, null, new TokenProperties());
    }

    public OnlineUserServiceImpl(OnlineUserSessionRepository sessionRepository, TokenManager tokenManager,
            LoginUserProvider loginUserProvider) {
        this(sessionRepository, tokenManager, loginUserProvider, new TokenProperties());
    }

    @Autowired
    public OnlineUserServiceImpl(OnlineUserSessionRepository sessionRepository, TokenManager tokenManager,
            LoginUserProvider loginUserProvider, TokenProperties tokenProperties) {
        this.sessionRepository = sessionRepository;
        this.tokenManager = tokenManager;
        this.tokenProperties = tokenProperties;
        this.loginUserProvider = loginUserProvider;
    }

    @Override
    public void recordLoginSession(Long userId, String userAccount, String userName, String userRole, String token,
            HttpServletRequest request) {
        if (userId == null || StringUtils.isBlank(token)) {
            return;
        }
        Date now = new Date();
        OnlineUserSession session = new OnlineUserSession();
        session.setSessionId(String.valueOf(userId));
        session.setUserId(userId);
        session.setLoginIp(request == null ? null : NetUtils.getIpAddress(request));
        session.setClientInfo(request == null ? null : request.getHeader("User-Agent"));
        session.setLoginTime(now);
        session.setLastAccessTime(now);
        session.setExpireTime(new Date(now.getTime() + tokenProperties.getExpireDurationSeconds() * 1000L));
        sessionRepository.save(session);
    }

    @Override
    public Page<OnlineUserVO> listOnlineUsers(OnlineUserQueryRequest request) {
        OnlineUserQueryRequest query = request == null ? new OnlineUserQueryRequest() : request;
        List<OnlineUserVO> matchedUsers = sessionRepository.listAll().stream()
                .filter(session -> matches(query, session))
                .sorted(Comparator.comparing(OnlineUserSession::getLastAccessTime,
                        Comparator.nullsLast(Date::compareTo)).reversed())
                .map(session -> toVO(session, loadLoginUserInfo(session.getUserId())))
                .filter(vo -> matchesLoginUser(query, vo))
                .collect(Collectors.toList());
        long total = matchedUsers.size();
        int page = Math.max(query.getPage(), 1);
        int pageSize = Math.max(query.getPageSize(), 1);
        int fromIndex = Math.min((page - 1) * pageSize, matchedUsers.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedUsers.size());
        Page<OnlineUserVO> pageResult = new Page<>(page, pageSize, total);
        pageResult.setRecords(matchedUsers.subList(fromIndex, toIndex));
        return pageResult;
    }

    @Override
    public void refreshLastAccess(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        Long userId = tokenManager.getUserId(token);
        OnlineUserSession session = sessionRepository.findByUserId(userId);
        if (session == null) {
            rebuildSessionMetadata(userId);
            return;
        }
        if (session.getLastAccessTime() == null) {
            return;
        }
        long intervalMillis = tokenProperties.getOnlineSessionRefreshIntervalSeconds() * 1000L;
        Date now = new Date();
        if (now.getTime() - session.getLastAccessTime().getTime() < intervalMillis) {
            return;
        }
        sessionRepository.refreshLastAccess(userId, now);
    }

    @Override
    public void removeByToken(String token) {
        sessionRepository.deleteByUserId(tokenManager.getUserId(token));
    }

    @Override
    public boolean forceLogout(String sessionId, String currentToken) {
        OnlineUserSession session = sessionRepository.findBySessionId(sessionId);
        String targetToken = session == null ? null : tokenManager.getTokenByUserId(session.getUserId());
        if (session == null || StringUtils.isBlank(targetToken)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "在线会话不存在或已失效");
        }
        if (StringUtils.equals(targetToken, currentToken)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能强制下线当前会话");
        }
        tokenManager.removeToken(targetToken);
        sessionRepository.deleteBySessionId(sessionId);
        return true;
    }

    /**
     * 判断在线会话是否匹配查询条件。
     */
    private boolean matches(OnlineUserQueryRequest query, OnlineUserSession session) {
        if (query.getUserId() != null && !query.getUserId().equals(session.getUserId())) {
            return false;
        }
        if (StringUtils.isNotBlank(query.getLoginIp())
                && !StringUtils.containsIgnoreCase(session.getLoginIp(), query.getLoginIp())) {
            return false;
        }
        if (query.getLoginStartTime() != null && before(session.getLoginTime(), query.getLoginStartTime())) {
            return false;
        }
        return query.getLoginEndTime() == null || !after(session.getLoginTime(), query.getLoginEndTime());
    }

    /**
     * 判断用户快照字段是否匹配查询条件。
     */
    private boolean matchesLoginUser(OnlineUserQueryRequest query, OnlineUserVO user) {
        if (StringUtils.isNotBlank(query.getUserAccount())
                && !StringUtils.containsIgnoreCase(user.getUserAccount(), query.getUserAccount())) {
            return false;
        }
        if (StringUtils.isNotBlank(query.getUserName())
                && !StringUtils.containsIgnoreCase(user.getUserName(), query.getUserName())) {
            return false;
        }
        return StringUtils.isBlank(query.getUserRole()) || StringUtils.equals(query.getUserRole(), user.getUserRole());
    }

    /**
     * 转换为在线用户列表视图，用户身份字段来自登录用户快照缓存。
     */
    private OnlineUserVO toVO(OnlineUserSession session, LoginUserInfo loginUserInfo) {
        OnlineUserVO vo = new OnlineUserVO();
        vo.setSessionId(session.getSessionId());
        vo.setUserId(session.getUserId());
        vo.setLoginIp(session.getLoginIp());
        vo.setClientInfo(session.getClientInfo());
        vo.setLoginTime(session.getLoginTime());
        vo.setLastAccessTime(session.getLastAccessTime());
        vo.setExpireTime(session.getExpireTime());
        if (loginUserInfo != null) {
            vo.setUserAccount(loginUserInfo.userAccount());
            vo.setUserName(loginUserInfo.userName());
            vo.setUserRole(loginUserInfo.userRole());
        }
        return vo;
    }

    /**
     * 加载登录用户快照，复用已有 login:user-info:* 缓存，避免在线会话重复保存用户信息。
     */
    private LoginUserInfo loadLoginUserInfo(Long userId) {
        return loginUserProvider == null ? null : loginUserProvider.loadLoginUser(userId);
    }

    /**
     * 为存量登录态补建新的轻量在线会话元数据，避免旧 Redis 结构迁移后在线列表暂时缺失用户。
     */
    private void rebuildSessionMetadata(Long userId) {
        if (userId == null) {
            return;
        }
        Date now = new Date();
        OnlineUserSession session = new OnlineUserSession();
        session.setSessionId(String.valueOf(userId));
        session.setUserId(userId);
        session.setLoginTime(now);
        session.setLastAccessTime(now);
        session.setExpireTime(new Date(now.getTime() + tokenProperties.getExpireDurationSeconds() * 1000L));
        sessionRepository.save(session);
    }

    /**
     * 判断时间是否早于边界。
     */
    private boolean before(Date value, Date boundary) {
        return value == null || value.before(boundary);
    }

    /**
     * 判断时间是否晚于边界。
     */
    private boolean after(Date value, Date boundary) {
        return value == null || value.after(boundary);
    }
}
