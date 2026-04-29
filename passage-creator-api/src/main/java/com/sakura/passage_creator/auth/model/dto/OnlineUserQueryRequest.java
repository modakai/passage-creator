package com.sakura.passage_creator.auth.model.dto;

import com.sakura.passage_creator.shared.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 在线用户分页查询请求。
 *
 * @author Sakura
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnlineUserQueryRequest extends PageRequest {

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
     * 登录开始时间。
     */
    private Date loginStartTime;

    /**
     * 登录结束时间。
     */
    private Date loginEndTime;
}
