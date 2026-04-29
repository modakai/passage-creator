package com.sakura.passage_creator.user.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.user.model.dto.UserQueryRequest;
import com.sakura.passage_creator.user.model.dto.UserUpdateRequest;
import com.sakura.passage_creator.user.model.entity.User;
import com.sakura.passage_creator.user.model.vo.UserVO;

import java.util.List;

/**
 * 用户服务
 *
 * @author sakura
 * @from sakura
 */
public interface UserService extends IService<User> {

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户
     * @return 用户视图
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息
     *
     * @param userList 用户列表
     * @return 用户视图列表
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 删除用户，包含内置超级管理员保护规则。
     *
     * @param id 用户 id
     * @return 是否删除成功
     */
    boolean removeUser(Long id);

    /**
     * 更新用户，包含内置超级管理员状态保护规则。
     *
     * @param userUpdateRequest 更新请求
     * @return 是否更新成功
     */
    boolean updateUser(UserUpdateRequest userUpdateRequest);

    /**
     * 当前登录用户修改密码。
     *
     * @param loginUser 当前登录用户
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @param checkPassword 确认密码
     * @return 是否修改成功
     */
    boolean updateMyPassword(User loginUser, String oldPassword, String newPassword, String checkPassword);

    /**
     * 将指定用户密码重置为系统默认密码。
     *
     * @param id 用户 id
     * @return 是否重置成功
     */
    boolean resetPassword(Long id);
}
