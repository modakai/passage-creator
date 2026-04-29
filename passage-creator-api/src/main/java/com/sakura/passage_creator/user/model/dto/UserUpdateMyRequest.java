package com.sakura.passage_creator.user.model.dto;

import com.sakura.passage_creator.user.model.entity.User;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新个人信息请求
 *
 * @author Sakura
 */
@Data
@AutoMapper(target = User.class, reverseConvertGenerate = false)
public class UserUpdateMyRequest implements Serializable {

    /**
     * 用户昵称
     */
    @NotBlank(message = "{validation.user.name.not_blank}")
    @Size(max = 20, message = "{validation.user.name.max}")
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    private static final long serialVersionUID = 1L;
}
