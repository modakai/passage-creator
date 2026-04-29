package com.sakura.passage_creator.observability.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.sakura.passage_creator.observability.config.ObservabilityProperties;
import com.sakura.passage_creator.observability.enums.ObservabilityEventLevelEnum;
import com.sakura.passage_creator.observability.enums.ObservabilityEventTypeEnum;
import com.sakura.passage_creator.observability.model.dto.ObservabilityEventQueryRequest;
import com.sakura.passage_creator.observability.model.dto.RequestObservationCommand;
import com.sakura.passage_creator.observability.model.entity.ObservabilityEvent;
import com.sakura.passage_creator.observability.model.vo.ApiSummaryVO;
import com.sakura.passage_creator.observability.model.vo.ErrorTrendBucketVO;
import com.sakura.passage_creator.observability.model.vo.ObservabilityEventVO;
import com.sakura.passage_creator.observability.repository.ObservabilityEventMapper;
import com.sakura.passage_creator.observability.service.ObservabilityEventService;
import com.sakura.passage_creator.observability.support.ObservabilitySanitizer;
import com.sakura.passage_creator.shared.common.ErrorCode;
import com.sakura.passage_creator.shared.exception.ThrowUtils;
import io.github.linpeilie.Converter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.sakura.passage_creator.observability.model.entity.table.ObservabilityEventTableDef.OBSERVABILITY_EVENT;

/**
 * 运维观测事件服务实现。
 *
 * @author Sakura
 */
@Service
@Slf4j
public class ObservabilityEventServiceImpl extends ServiceImpl<ObservabilityEventMapper, ObservabilityEvent>
        implements ObservabilityEventService {

    /**
     * 异步事件队列容量。
     */
    private static final int EVENT_QUEUE_CAPACITY = 1000;

    /**
     * 异步写入执行器，避免观测事件影响主请求；队列有界，避免数据库异常时拖垮业务线程。
     */
    private final ExecutorService eventExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(EVENT_QUEUE_CAPACITY), runnable -> {
        Thread thread = new Thread(runnable, "observability-event-writer");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    /**
     * 运维事件 Mapper。
     */
    private final ObservabilityEventMapper eventMapper;

    /**
     * 可观测性配置。
     */
    private final ObservabilityProperties properties;

    /**
     * 摘要脱敏工具。
     */
    private final ObservabilitySanitizer sanitizer = new ObservabilitySanitizer();

    /**
     * MapStruct Plus 转换器，用于替代反射式 BeanUtils 属性复制。
     */
    private final Converter converter;

    public ObservabilityEventServiceImpl(ObservabilityEventMapper eventMapper, ObservabilityProperties properties,
            Converter converter) {
        this.eventMapper = eventMapper;
        this.properties = properties;
        this.converter = converter;
    }

    @Override
    public void recordRequest(RequestObservationCommand command) {
        if (command == null || StringUtils.isBlank(command.getRequestPath())) {
            return;
        }
        if (isSlowRequest(command)) {
            saveSafely(buildRequestEvent(command, ObservabilityEventTypeEnum.SLOW_API));
        }
        if (isErrorRequest(command)) {
            saveSafely(buildRequestEvent(command, ObservabilityEventTypeEnum.API_ERROR));
        }
    }

    @Override
    public ObservabilityEvent saveEvent(ObservabilityEvent event) {
        ThrowUtils.throwIf(event == null, ErrorCode.PARAMS_ERROR);
        if (event.getEventTime() == null) {
            event.setEventTime(new Date());
        }
        event.setExceptionSummary(sanitizer.sanitize(event.getExceptionSummary()));
        event.setDetail(sanitizer.sanitize(event.getDetail()));
        eventMapper.insertSelective(event);
        return event;
    }

    @Override
    public void saveEventAsync(ObservabilityEvent event) {
        try {
            eventExecutor.execute(() -> {
                try {
                    saveEvent(event);
                } catch (Exception e) {
                    log.error("save observability event failed", e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("observability event queue is full, event dropped: {}", event == null ? null : event.getTitle());
        }
    }

    @Override
    public Page<ObservabilityEventVO> listSlowApiEvents(ObservabilityEventQueryRequest request) {
        ObservabilityEventQueryRequest query = request == null ? new ObservabilityEventQueryRequest() : request;
        QueryWrapper wrapper = getBaseQueryWrapper(query)
                .and(OBSERVABILITY_EVENT.EVENT_TYPE.eq(ObservabilityEventTypeEnum.SLOW_API.getValue()))
                .orderBy(OBSERVABILITY_EVENT.EVENT_TIME, false)
                .orderBy(OBSERVABILITY_EVENT.DURATION_MILLIS, false);
        Page<ObservabilityEvent> page = this.page(Page.of(query.getPage(), query.getPageSize()), wrapper);
        Page<ObservabilityEventVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(page.getRecords().stream().map(this::getEventVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<ObservabilityEventVO> listSecurityEvents(ObservabilityEventQueryRequest request) {
        ObservabilityEventQueryRequest query = request == null ? new ObservabilityEventQueryRequest() : request;
        QueryWrapper wrapper = getBaseQueryWrapper(query)
                .and(OBSERVABILITY_EVENT.EVENT_TYPE.in(List.of(
                        ObservabilityEventTypeEnum.LOGIN_FAILURE.getValue(),
                        ObservabilityEventTypeEnum.ABNORMAL_IP.getValue(),
                        ObservabilityEventTypeEnum.FORCE_LOGOUT.getValue(),
                        ObservabilityEventTypeEnum.SECURITY_ALERT.getValue())))
                .orderBy(OBSERVABILITY_EVENT.EVENT_TIME, false);
        Page<ObservabilityEvent> page = this.page(Page.of(query.getPage(), query.getPageSize()), wrapper);
        Page<ObservabilityEventVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(page.getRecords().stream().map(this::getEventVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public ApiSummaryVO getApiSummary(ObservabilityEventQueryRequest request) {
        QueryWrapper wrapper = getBaseQueryWrapper(request == null ? new ObservabilityEventQueryRequest() : request);
        List<ObservabilityEvent> events = this.list(wrapper.and(OBSERVABILITY_EVENT.EVENT_TYPE.in(List.of(
                ObservabilityEventTypeEnum.SLOW_API.getValue(), ObservabilityEventTypeEnum.API_ERROR.getValue()))));
        ApiSummaryVO vo = new ApiSummaryVO();
        vo.setSlowApiCount(events.stream()
                .filter(event -> ObservabilityEventTypeEnum.SLOW_API.getValue().equals(event.getEventType()))
                .count());
        vo.setErrorCount(events.stream()
                .filter(event -> ObservabilityEventTypeEnum.API_ERROR.getValue().equals(event.getEventType()))
                .count());
        vo.setAverageSlowDurationMillis(events.stream()
                .filter(event -> ObservabilityEventTypeEnum.SLOW_API.getValue().equals(event.getEventType()))
                .map(ObservabilityEvent::getDurationMillis)
                .filter(value -> value != null && value > 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));
        return vo;
    }

    @Override
    public List<ErrorTrendBucketVO> listErrorTrend(ObservabilityEventQueryRequest request) {
        QueryWrapper wrapper = getBaseQueryWrapper(request == null ? new ObservabilityEventQueryRequest() : request)
                .and(OBSERVABILITY_EVENT.EVENT_TYPE.eq(ObservabilityEventTypeEnum.API_ERROR.getValue()))
                .orderBy(OBSERVABILITY_EVENT.EVENT_TIME, true);
        Map<String, ErrorTrendBucketVO> bucketMap = new LinkedHashMap<>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
        for (ObservabilityEvent event : this.list(wrapper)) {
            String bucket = formatter.format(event.getEventTime() == null ? new Date() : event.getEventTime());
            ErrorTrendBucketVO vo = bucketMap.computeIfAbsent(bucket, key -> {
                ErrorTrendBucketVO item = new ErrorTrendBucketVO();
                item.setBucket(key);
                return item;
            });
            if (event.getStatusCode() != null && event.getStatusCode() >= 500) {
                vo.setServerErrorCount(vo.getServerErrorCount() + 1);
            } else if (event.getStatusCode() != null && event.getStatusCode() >= 400) {
                vo.setClientErrorCount(vo.getClientErrorCount() + 1);
            }
            if (StringUtils.isNotBlank(event.getExceptionSummary())) {
                vo.setExceptionCount(vo.getExceptionCount() + 1);
            }
        }
        return new ArrayList<>(bucketMap.values());
    }

    @Override
    public ObservabilityEventVO getEventVO(ObservabilityEvent event) {
        if (event == null) {
            return null;
        }
        return converter.convert(event, ObservabilityEventVO.class);
    }

    /**
     * 构造通用查询条件。
     */
    private QueryWrapper getBaseQueryWrapper(ObservabilityEventQueryRequest request) {
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.where(OBSERVABILITY_EVENT.EVENT_TYPE.eq(request.getEventType(), StringUtils.isNotBlank(request.getEventType())));
        wrapper.and(OBSERVABILITY_EVENT.EVENT_LEVEL.eq(request.getEventLevel(), StringUtils.isNotBlank(request.getEventLevel())));
        wrapper.and(OBSERVABILITY_EVENT.REQUEST_PATH.like(request.getRequestPath(),
                StringUtils.isNotBlank(request.getRequestPath())));
        wrapper.and(OBSERVABILITY_EVENT.IP_ADDRESS.eq(request.getIpAddress(), StringUtils.isNotBlank(request.getIpAddress())));
        wrapper.and(OBSERVABILITY_EVENT.ACCOUNT_IDENTIFIER.like(request.getAccountIdentifier(),
                StringUtils.isNotBlank(request.getAccountIdentifier())));
        wrapper.and(OBSERVABILITY_EVENT.EVENT_TIME.ge(request.getStartTime(), request.getStartTime() != null));
        wrapper.and(OBSERVABILITY_EVENT.EVENT_TIME.le(request.getEndTime(), request.getEndTime() != null));
        return wrapper;
    }

    /**
     * 判断是否为慢接口。
     */
    private boolean isSlowRequest(RequestObservationCommand command) {
        return command.getDurationMillis() != null
                && command.getDurationMillis() >= properties.getSlowApiThresholdMillis();
    }

    /**
     * 判断是否为错误请求。
     */
    private boolean isErrorRequest(RequestObservationCommand command) {
        return command.getThrowable() != null
                || command.getStatusCode() != null && command.getStatusCode() >= 400;
    }

    /**
     * 构造请求事件。
     */
    private ObservabilityEvent buildRequestEvent(RequestObservationCommand command, ObservabilityEventTypeEnum type) {
        ObservabilityEvent event = new ObservabilityEvent();
        ObservabilityEventTypeEnum resolvedType = resolveRequestEventType(command, type);
        event.setEventType(resolvedType.getValue());
        event.setEventLevel(ObservabilityEventTypeEnum.API_ERROR == resolvedType
                ? ObservabilityEventLevelEnum.ERROR.getValue()
                : ObservabilityEventLevelEnum.WARNING.getValue());
        event.setTitle(resolveRequestEventTitle(resolvedType));
        event.setSubject(command.getRequestPath());
        event.setRequestPath(command.getRequestPath());
        event.setHttpMethod(command.getHttpMethod());
        event.setStatusCode(command.getStatusCode());
        event.setDurationMillis(command.getDurationMillis());
        event.setUserId(command.getUserId());
        event.setAccountIdentifier(command.getAccountIdentifier());
        event.setIpAddress(command.getIpAddress());
        event.setExceptionSummary(buildExceptionSummary(command.getThrowable()));
        event.setEventTime(command.getEventTime() == null ? new Date() : command.getEventTime());
        return event;
    }

    /**
     * 强制下线接口单独归类为安全事件。
     */
    private ObservabilityEventTypeEnum resolveRequestEventType(RequestObservationCommand command,
            ObservabilityEventTypeEnum type) {
        if (StringUtils.contains(command.getRequestPath(), "/online/user/force-logout")) {
            return ObservabilityEventTypeEnum.FORCE_LOGOUT;
        }
        return type;
    }

    /**
     * 获取请求事件标题。
     */
    private String resolveRequestEventTitle(ObservabilityEventTypeEnum type) {
        if (ObservabilityEventTypeEnum.API_ERROR == type) {
            return "接口错误";
        }
        if (ObservabilityEventTypeEnum.FORCE_LOGOUT == type) {
            return "强制下线";
        }
        return "慢接口";
    }

    /**
     * 异步安全写入事件。
     */
    private void saveSafely(ObservabilityEvent event) {
        saveEventAsync(event);
    }

    /**
     * 构造异常摘要，避免保存完整堆栈。
     */
    private String buildExceptionSummary(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return sanitizer.sanitize(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
    }

    /**
     * 关闭异步事件执行器。
     */
    @PreDestroy
    public void shutdownEventExecutor() {
        eventExecutor.shutdown();
    }
}
