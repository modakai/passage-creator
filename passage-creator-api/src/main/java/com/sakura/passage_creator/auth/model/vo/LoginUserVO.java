package com.sakura.passage_creator.auth.model.vo;

import com.sakura.passage_creator.user.model.entity.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已登录用户视图，已脱敏
 *
 * @author Sakura
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 用户 id
     */
    private Long id;

    /**
     * 用户账号。
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

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
     * 登录 token，后续请求需要携带该值。
     */
    private String token;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
