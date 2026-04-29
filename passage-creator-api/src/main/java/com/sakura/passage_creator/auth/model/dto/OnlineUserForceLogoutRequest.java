package com.sakura.passage_creator.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 强制下线请求。
 *
 * @author Sakura
 */
@Data
public class OnlineUserForceLogoutRequest {

    /**
     * 要下线的在线会话标识。
     */
    @NotBlank(message = "在线会话标识不能为空")
    private String sessionId;
}
