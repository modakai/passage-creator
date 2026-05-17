package com.sakura.passage_creator.creation.workflow.service;

import com.sakura.passage_creator.article.manager.SseEmitterManager;
import com.sakura.passage_creator.article.workflow.ArticleWorkflowNodeType;
import com.sakura.passage_creator.creation.workflow.WorkflowEvent;
import com.sakura.passage_creator.creation.workflow.enums.CreationTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.WorkflowEventTypeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Workflow SSE 发布测试，确保重连补发的人机交互事件能兼容旧文章前端。
 */
class SseWorkflowEventPublisherTest {

    @Test
    void shouldMapTitleWaitingUserEventToLegacyTitleGeneratedMessage() {
        SseEmitterManager sseEmitterManager = mock(SseEmitterManager.class);
        SseWorkflowEventPublisher publisher = new SseWorkflowEventPublisher(sseEmitterManager);

        publisher.publish(WorkflowEvent.builder()
                .type(WorkflowEventTypeEnum.NODE_WAITING_USER.getValue())
                .taskId("task-1")
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType(ArticleWorkflowNodeType.TITLE_CONFIRM.getValue())
                .payload(Map.of("inputSnapshot", Map.of("titleOptions", List.of("标题 A"))))
                .eventTime(LocalDateTime.now())
                .build());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(sseEmitterManager, times(2)).send(org.mockito.ArgumentMatchers.eq("task-1"), messageCaptor.capture());
        assertThat(messageCaptor.getAllValues().get(1)).contains("TITLES_GENERATED", "标题 A");
    }
}
