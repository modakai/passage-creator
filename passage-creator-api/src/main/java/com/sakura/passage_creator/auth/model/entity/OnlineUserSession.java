package com.sakura.passage_creator.auth.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 在线用户会话元数据，只保存在线状态独有字段，用户基础信息复用登录用户快照缓存。
 *
 * @author Sakura
 */
@Data
public class OnlineUserSession implements Serializable {

    /**
     * 在线会话标识，用于前端列表操作。
     */
    private String sessionId;

    /**
     * 用户 id。
     */
    private Long userId;

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
