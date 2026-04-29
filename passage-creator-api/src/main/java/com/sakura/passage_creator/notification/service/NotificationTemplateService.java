package com.sakura.passage_creator.notification.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateAddRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateQueryRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateUpdateRequest;
import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import com.sakura.passage_creator.notification.model.vo.NotificationTemplateVO;

import java.util.List;

/**
 * 消息通知模板服务。
 *
 * @author Sakura
 */
public interface NotificationTemplateService extends IService<NotificationTemplate> {

    /**
     * 新增模板。
     */
    Long addTemplate(NotificationTemplateAddRequest request);

    /**
     * 更新模板。
     */
    boolean updateTemplate(NotificationTemplateUpdateRequest request);

    /**
     * 启用模板。
     */
    boolean enableTemplate(Long id);

    /**
     * 停用模板。
     */
    boolean disableTemplate(Long id);

    /**
     * 获取事件启用模板。
     */
    NotificationTemplate getEnabledByEventType(String eventType);

    /**
     * 构造查询条件。
     */
    QueryWrapper getQueryWrapper(NotificationTemplateQueryRequest request);

    /**
     * 转换返回对象。
     */
    NotificationTemplateVO getTemplateVO(NotificationTemplate template);

    /**
     * 转换返回对象列表。
     */
    List<NotificationTemplateVO> getTemplateVO(List<NotificationTemplate> templateList);
}
