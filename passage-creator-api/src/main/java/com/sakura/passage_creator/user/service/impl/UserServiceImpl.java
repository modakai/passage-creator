package com.sakura.passage_creator.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.infrastructure.auth.LoginUserCache;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.constant.CommonConstant;
import com.sakura.passage_creator.shared.constant.UserConstant;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import com.sakura.passage_creator.user.model.dto.UserQueryRequest;
import com.sakura.passage_creator.user.model.dto.UserUpdateRequest;
import com.sakura.passage_creator.user.model.entity.User;
import com.sakura.passage_creator.user.model.vo.UserVO;
import com.sakura.passage_creator.user.repository.UserMapper;
import com.sakura.passage_creator.user.service.UserService;
import io.github.linpeilie.Converter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.DigestUtils;

import static com.sakura.passage_creator.user.model.entity.table.UserTableDef.USER;

/**
 * 用户服务实现
 *
 * @author sakura
 * @from sakura
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 登录用户快照缓存。
     */
    private final LoginUserCache loginUserCache;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    private final Converter converter;

    public UserServiceImpl(LoginUserCache loginUserCache, Converter converter) {
        this.loginUserCache = loginUserCache;
        this.converter = converter;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        return converter.convert(user, UserVO.class);
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        Integer status = userQueryRequest.getStatus();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(USER.ID.eq(id, id != null));
        queryWrapper.and(USER.UNION_ID.eq(unionId, StringUtils.isNotBlank(unionId)));
        queryWrapper.and(USER.MP_OPEN_ID.eq(mpOpenId, StringUtils.isNotBlank(mpOpenId)));
        queryWrapper.and(USER.USER_ROLE.eq(userRole, StringUtils.isNotBlank(userRole)));
        queryWrapper.and(USER.STATUS.eq(status, status != null));
        queryWrapper.and(USER.USER_PROFILE.like(userProfile, StringUtils.isNotBlank(userProfile)));
        queryWrapper.and(USER.USER_NAME.like(userName, StringUtils.isNotBlank(userName)));
        QueryColumn sortColumn = resolveSortColumn(sortField);
        if (sortColumn != null) {
            // MyBatis-Flex 排序统一使用 APT 字段，避免直接拼接客户端字段名。
            queryWrapper.orderBy(sortColumn, CommonConstant.SORT_ORDER_ASC.equals(sortOrder));
        }
        return queryWrapper;
    }

    /**
     * 将客户端排序字段转换为用户表 APT 字段。
     */
    private QueryColumn resolveSortColumn(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }
        return switch (sortField) {
            case "id" -> USER.ID;
            case "union_id" -> USER.UNION_ID;
            case "mp_open_id" -> USER.MP_OPEN_ID;
            case "user_role" -> USER.USER_ROLE;
            case "status" -> USER.STATUS;
            case "user_profile" -> USER.USER_PROFILE;
            case "user_name" -> USER.USER_NAME;
            case "user_account" -> USER.USER_ACCOUNT;
            case "create_time" -> USER.CREATE_TIME;
            case "update_time" -> USER.UPDATE_TIME;
            default -> null;
        };
    }

    @Override
    public boolean removeUser(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 id 非法");
        }
        User oldUser = this.getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 内置超级管理员账号承担系统兜底管理职责，禁止通过用户管理删除。
        ThrowUtils.throwIf(UserConstant.PROTECTED_SUPER_ADMIN_ACCOUNT.equals(oldUser.getUserAccount()),
                ErrorCode.FORBIDDEN_ERROR, "内置超级管理员 sakura 不允许删除");

        boolean result = this.removeById(id);
        if (result) {
            loginUserCache.evict(id);
        }
        return result;
    }

    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        User oldUser = this.getById(userUpdateRequest.getId());
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 内置超级管理员必须保持启用状态，避免系统失去兜底管理员。
        ThrowUtils.throwIf(UserConstant.PROTECTED_SUPER_ADMIN_ACCOUNT.equals(oldUser.getUserAccount())
                        && UserConstant.STATUS_DISABLED.equals(userUpdateRequest.getStatus()),
                ErrorCode.FORBIDDEN_ERROR, "内置超级管理员 sakura 不允许禁用");

        User user = converter.convert(userUpdateRequest, User.class);
        boolean result = this.updateById(user);
        if (result) {
            loginUserCache.evict(userUpdateRequest.getId());
        }
        return result;
    }

    @Override
    public boolean updateMyPassword(User loginUser, String oldPassword, String newPassword, String checkPassword) {
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(StringUtils.isAnyBlank(oldPassword, newPassword, checkPassword), ErrorCode.PARAMS_ERROR,
                "auth.param.blank");
        ThrowUtils.throwIf(oldPassword.equals(newPassword), ErrorCode.PARAMS_ERROR, "auth.password.same");
        ThrowUtils.throwIf(!newPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "auth.password.not_match");

        String encryptedOldPassword = DigestUtils
                .md5DigestAsHex((UserConstant.PASSWORD_SALT + oldPassword).getBytes());
        ThrowUtils.throwIf(!encryptedOldPassword.equals(loginUser.getUserPassword()), ErrorCode.PARAMS_ERROR,
                "auth.password.old.invalid");

        User user = new User();
        user.setId(loginUser.getId());
        user.setUserPassword(DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + newPassword).getBytes()));
        return this.updateById(user);
    }

    @Override
    public boolean resetPassword(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 id 非法");
        }
        User oldUser = this.getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);

        User user = new User();
        user.setId(id);
        // 管理员重置密码时统一恢复到系统默认密码。
        user.setUserPassword(DigestUtils.md5DigestAsHex(
                (UserConstant.PASSWORD_SALT + UserConstant.DEFAULT_PASSWORD).getBytes()));
        return this.updateById(user);
    }
}
