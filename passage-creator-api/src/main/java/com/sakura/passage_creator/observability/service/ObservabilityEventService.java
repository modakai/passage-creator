package com.sakura.passage_creator.observability.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.sakura.passage_creator.observability.model.dto.ObservabilityEventQueryRequest;
import com.sakura.passage_creator.observability.model.dto.RequestObservationCommand;
import com.sakura.passage_creator.observability.model.entity.ObservabilityEvent;
import com.sakura.passage_creator.observability.model.vo.ApiSummaryVO;
import com.sakura.passage_creator.observability.model.vo.ErrorTrendBucketVO;
import com.sakura.passage_creator.observability.model.vo.ObservabilityEventVO;

import java.util.List;

/**
 * 运维观测事件服务。
 *
 * @author Sakura
 */
public interface ObservabilityEventService extends IService<ObservabilityEvent> {

    /**
     * 记录请求观测事实。
     */
    void recordRequest(RequestObservationCommand command);

    /**
     * 保存安全事件。
     */
    ObservabilityEvent saveEvent(ObservabilityEvent event);

    /**
     * 异步保存运维事件。
     */
    void saveEventAsync(ObservabilityEvent event);

    /**
     * 查询慢接口事件。
     */
    Page<ObservabilityEventVO> listSlowApiEvents(ObservabilityEventQueryRequest request);

    /**
     * 查询安全事件。
     */
    Page<ObservabilityEventVO> listSecurityEvents(ObservabilityEventQueryRequest request);

    /**
     * 获取接口质量摘要。
     */
    ApiSummaryVO getApiSummary(ObservabilityEventQueryRequest request);

    /**
     * 获取错误趋势。
     */
    List<ErrorTrendBucketVO> listErrorTrend(ObservabilityEventQueryRequest request);

    /**
     * 转换事件视图。
     */
    ObservabilityEventVO getEventVO(ObservabilityEvent event);
}
