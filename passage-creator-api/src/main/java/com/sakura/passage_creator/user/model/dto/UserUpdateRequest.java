package com.sakura.passage_creator.user.model.dto;

import com.sakura.passage_creator.user.model.entity.User;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = User.class, reverseConvertGenerate = false)
public class UserUpdateRequest implements Serializable {

    /**
     * id
     */
    @NotNull(message = "{validation.user.id.not_null}")
    @Positive(message = "{validation.user.id.positive}")
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色，user/admin/ban
     */
    private String userRole;

    /**
     * 状态：1 启用，0 禁用
     */
    @NotNull(message = "{validation.user.status.not_null}")
    private Integer status;

    /**
     * 禁用原因，用于触发消息通知模板中的 {reason} 变量。
     */
    private String disableReason;

    private static final long serialVersionUID = 1L;
}
