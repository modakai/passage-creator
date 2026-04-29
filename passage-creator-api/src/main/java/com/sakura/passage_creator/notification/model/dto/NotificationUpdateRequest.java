package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.notification.model.entity.Notification;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新通知公告请求。
 *
 * @author Sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AutoMapper(target = Notification.class, reverseConvertGenerate = false)
public class NotificationUpdateRequest extends NotificationAddRequest {

    /**
     * 通知 id。
     */
    @NotNull(message = "通知 id 不能为空")
    private Long id;

    private static final long serialVersionUID = 1L;
}
