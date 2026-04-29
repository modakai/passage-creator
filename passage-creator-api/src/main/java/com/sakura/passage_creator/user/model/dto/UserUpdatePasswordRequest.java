package com.sakura.passage_creator.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户修改密码请求。
 *
 * @author Sakura
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * 旧密码。
     */
    @NotBlank(message = "{validation.user.old_password.not_blank}")
    @Size(min = 8, message = "{validation.user.old_password.min}")
    private String oldPassword;

    /**
     * 新密码。
     */
    @NotBlank(message = "{validation.user.new_password.not_blank}")
    @Size(min = 8, message = "{validation.user.new_password.min}")
    private String newPassword;

    /**
     * 确认密码。
     */
    @NotBlank(message = "{validation.user.check_password.not_blank}")
    @Size(min = 8, message = "{validation.user.check_password.min}")
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}
