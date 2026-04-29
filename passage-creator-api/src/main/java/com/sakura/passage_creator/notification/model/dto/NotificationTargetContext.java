package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.shared.context.LoginUserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知可见性判断上下文。
 *
 * @author Sakura
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTargetContext {

    /**
     * 当前访问端：admin/app。
     */
    private String receiverType;

    /**
     * 当前登录用户快照。
     */
    private LoginUserInfo user;
}
