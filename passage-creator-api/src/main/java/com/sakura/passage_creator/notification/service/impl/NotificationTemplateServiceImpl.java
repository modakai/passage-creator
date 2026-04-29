package com.sakura.passage_creator.notification.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateAddRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateQueryRequest;
import com.sakura.passage_creator.notification.model.dto.NotificationTemplateUpdateRequest;
import com.sakura.passage_creator.notification.model.entity.NotificationTemplate;
import com.sakura.passage_creator.notification.model.vo.NotificationTemplateVO;
import com.sakura.passage_creator.notification.repository.NotificationTemplateMapper;
import com.sakura.passage_creator.notification.service.NotificationTemplateService;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.BusinessException;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.notification.model.entity.table.NotificationTemplateTableDef.NOTIFICATION_TEMPLATE;

/**
 * 消息通知模板服务实现。
 *
 * @author Sakura
 */
@Service
public class NotificationTemplateServiceImpl extends ServiceImpl<NotificationTemplateMapper, NotificationTemplate>
        implements NotificationTemplateService {

    @Resource
    private NotificationTemplateRenderer templateRenderer;

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    @Resource
    private Converter converter;

    @Override
    public Long addTemplate(NotificationTemplateAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        NotificationTemplate template = converter.convert(request, NotificationTemplate.class);
        validTemplate(template);
        if (template.getEnabled() == null) {
            template.setEnabled(0);
        }
        boolean result = this.save(template);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return template.getId();
    }

    @Override
    public boolean updateTemplate(NotificationTemplateUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(this.getById(request.getId()) == null, ErrorCode.NOT_FOUND_ERROR);
        NotificationTemplate template = converter.convert(request, NotificationTemplate.class);
        validTemplate(template);
        return this.updateById(template);
    }

    @Override
    public boolean enableTemplate(Long id) {
        return updateEnabled(id, 1);
    }

    @Override
    public boolean disableTemplate(Long id) {
        return updateEnabled(id, 0);
    }

    @Override
    public NotificationTemplate getEnabledByEventType(String eventType) {
        if (StringUtils.isBlank(eventType)) {
            return null;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(NOTIFICATION_TEMPLATE.EVENT_TYPE.eq(eventType))
                .and(NOTIFICATION_TEMPLATE.ENABLED.eq(1))
                .orderBy(NOTIFICATION_TEMPLATE.ID, false);
        return this.getOne(queryWrapper);
    }

    @Override
    public QueryWrapper getQueryWrapper(NotificationTemplateQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.where(NOTIFICATION_TEMPLATE.TEMPLATE_CODE.like(request.getTemplateCode(),
                StringUtils.isNotBlank(request.getTemplateCode())));
        queryWrapper.and(NOTIFICATION_TEMPLATE.EVENT_TYPE.eq(request.getEventType(),
                StringUtils.isNotBlank(request.getEventType())));
        queryWrapper.and(NOTIFICATION_TEMPLATE.ENABLED.eq(request.getEnabled(), request.getEnabled() != null));
        queryWrapper.orderBy(NOTIFICATION_TEMPLATE.ID, false);
        return queryWrapper;
    }

    @Override
    public NotificationTemplateVO getTemplateVO(NotificationTemplate template) {
        if (template == null) {
            return null;
        }
        return converter.convert(template, NotificationTemplateVO.class);
    }

    @Override
    public List<NotificationTemplateVO> getTemplateVO(List<NotificationTemplate> templateList) {
        if (CollUtil.isEmpty(templateList)) {
            return new ArrayList<>();
        }
        return templateList.stream().map(this::getTemplateVO).collect(Collectors.toList());
    }

    /**
     * 更新模板启用状态。
     */
    private boolean updateEnabled(Long id, Integer enabled) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(this.getById(id) == null, ErrorCode.NOT_FOUND_ERROR);
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setEnabled(enabled);
        return this.updateById(template);
    }

    /**
     * 校验模板参数。
     */
    private void validTemplate(NotificationTemplate template) {
        if (StringUtils.isAnyBlank(template.getTemplateCode(), template.getEventType(),
                template.getTitleTemplate(), template.getContentTemplate(), template.getReceiverType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        templateRenderer.validateTemplateVariables(template.getTitleTemplate(), template.getVariableSchema());
        templateRenderer.validateTemplateVariables(template.getContentTemplate(), template.getVariableSchema());
    }
}
