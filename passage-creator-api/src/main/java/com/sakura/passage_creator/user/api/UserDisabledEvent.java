package com.sakura.passage_creator.user.api;

/**
 * 用户被禁用事件，用于让通知等外部模块响应用户状态变化。
 *
 * @param userId 被禁用用户 id
 * @param disableReason 禁用原因
 */
public record UserDisabledEvent(Long userId, String disableReason) {
}
