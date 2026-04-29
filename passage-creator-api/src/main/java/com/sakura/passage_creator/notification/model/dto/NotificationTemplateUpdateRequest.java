package com.sakura.passage_creator.notification.model.dto;

import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新消息通知模板请求。
 *
 * @author Sakura
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AutoMapper(target = NotificationTemplate.class, reverseConvertGenerate = false)
public class NotificationTemplateUpdateRequest extends NotificationTemplateAddRequest {

    /**
     * 模板 id。
     */
    @NotNull(message = "模板 id 不能为空")
    private Long id;

    private static final long serialVersionUID = 1L;
}
