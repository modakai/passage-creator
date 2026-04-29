package com.sakura.passage_creator.auth.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 在线用户列表视图，不包含完整 token。
 *
 * @author Sakura
 */
@Data
public class OnlineUserVO implements Serializable {

    /**
     * 在线会话标识。
     */
    private String sessionId;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 用户账号。
     */
    private String userAccount;

    /**
     * 用户昵称。
     */
    private String userName;

    /**
     * 用户角色。
     */
    private String userRole;

    /**
     * 登录 IP。
     */
    private String loginIp;

    /**
     * 客户端信息。
     */
    private String clientInfo;

    /**
     * 登录时间。
     */
    private Date loginTime;

    /**
     * 最近访问时间。
     */
    private Date lastAccessTime;

    /**
     * 过期时间。
     */
    private Date expireTime;

    private static final long serialVersionUID = 1L;
}
