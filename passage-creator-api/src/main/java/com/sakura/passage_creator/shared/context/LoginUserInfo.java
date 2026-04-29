package com.sakura.passage_creator.shared.context;

/**
 * 当前请求登录用户快照，只保留跨模块通用身份字段。
 *
 * @param userId 用户 id
 * @param userAccount 用户账号
 * @param userName 用户昵称
 * @param userRole 用户角色
 */
public record LoginUserInfo(Long userId, String userAccount, String userName, String userRole) {
}
