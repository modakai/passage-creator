package com.sakura.passage_creator.user.model.dto;

import com.sakura.passage_creator.user.model.entity.User;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户创建请求
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = User.class, reverseConvertGenerate = false)
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    @NotBlank(message = "{validation.user.account.not_blank}")
    @Size(min = 4, message = "{validation.user.account.min}")
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户角色：user, admin
     */
    private String userRole;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 状态：1 启用，0 禁用
     */
    @NotNull(message = "{validation.user.status.not_null}")
    private Integer status;

    @Serial
    private static final long serialVersionUID = 1L;
}
