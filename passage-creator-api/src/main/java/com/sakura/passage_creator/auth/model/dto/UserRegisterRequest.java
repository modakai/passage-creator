package com.sakura.passage_creator.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author Sakura
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账号
     */
    @NotBlank(message = "{validation.user.account.not_blank}")
    @Size(min = 4, message = "{validation.user.account.min}")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotBlank(message = "{validation.user.password.not_blank}")
    @Size(min = 8, message = "{validation.user.password.min}")
    private String userPassword;

    /**
     * 校验密码
     */
    @NotBlank(message = "{validation.user.check_password.not_blank}")
    @Size(min = 8, message = "{validation.user.check_password.min}")
    private String checkPassword;
}
