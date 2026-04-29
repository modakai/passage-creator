package com.sakura.passage_creator.user.model.vo;

import com.sakura.passage_creator.user.model.entity.User;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图，已脱敏
 *
 * @author sakura
 * @from sakura
 */
@Data
@AutoMapper(target = User.class)
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色，user/admin/ban
     */
    private String userRole;

    /**
     * 状态：1 启用，0 禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
