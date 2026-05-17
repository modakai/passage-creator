package com.sakura.passage_creator.creation.workflow;

import com.sakura.passage_creator.creation.workflow.enums.CreationTypeEnum;
import com.sakura.passage_creator.creation.workflow.enums.HumanTaskStatusEnum;
import com.sakura.passage_creator.creation.workflow.service.InMemoryHumanTaskStore;
import com.sakura.passage_creator.creation.workflow.service.WorkflowHumanTaskService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 人工任务测试，覆盖 Human-in-the-Loop 的服务端校验边界。
 */
class HumanTaskServiceTest {

    @Test
    void shouldCompleteWaitingHumanTaskWithExpectedVersion() {
        WorkflowHumanTaskService service = new WorkflowHumanTaskService(new InMemoryHumanTaskStore());
        WorkflowHumanTask task = service.createWaitingTask(WorkflowHumanTaskCreateCommand.builder()
                .taskId("task-1")
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType("TITLE_CONFIRM")
                .assigneeUserId(1001L)
                .inputSnapshotJson("{\"titleOptions\":[]}")
                .formSchemaJson("{\"type\":\"TITLE_CONFIRM\"}")
                .build());

        WorkflowHumanTask completed = service.completeTask(WorkflowHumanTaskCompleteCommand.builder()
                .taskId("task-1")
                .nodeType("TITLE_CONFIRM")
                .userId(1001L)
                .version(task.getVersion())
                .result(Map.of("selectedMainTitle", "主标题", "selectedSubTitle", "副标题"))
                .build());

        assertThat(completed.getStatus()).isEqualTo(HumanTaskStatusEnum.COMPLETED.getValue());
        assertThat(completed.getResultJson()).contains("selectedMainTitle");
    }

    @Test
    void shouldRejectDuplicateOrStaleHumanTaskCompletion() {
        WorkflowHumanTaskService service = new WorkflowHumanTaskService(new InMemoryHumanTaskStore());
        WorkflowHumanTask task = service.createWaitingTask(WorkflowHumanTaskCreateCommand.builder()
                .taskId("task-1")
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType("TITLE_CONFIRM")
                .assigneeUserId(1001L)
                .inputSnapshotJson("{}")
                .formSchemaJson("{}")
                .build());

        service.completeTask(WorkflowHumanTaskCompleteCommand.builder()
                .taskId("task-1")
                .nodeType("TITLE_CONFIRM")
                .userId(1001L)
                .version(task.getVersion())
                .result(Map.of("selectedMainTitle", "主标题"))
                .build());

        assertThatThrownBy(() -> service.completeTask(WorkflowHumanTaskCompleteCommand.builder()
                .taskId("task-1")
                .nodeType("TITLE_CONFIRM")
                .userId(1001L)
                .version(task.getVersion())
                .result(Map.of("selectedMainTitle", "另一个标题"))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("人工任务不处于等待状态");
    }

    @Test
    void shouldRejectWrongUserHumanTaskCompletion() {
        WorkflowHumanTaskService service = new WorkflowHumanTaskService(new InMemoryHumanTaskStore());
        WorkflowHumanTask task = service.createWaitingTask(WorkflowHumanTaskCreateCommand.builder()
                .taskId("task-1")
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType("TITLE_CONFIRM")
                .assigneeUserId(1001L)
                .inputSnapshotJson("{}")
                .formSchemaJson("{}")
                .build());

        assertThatThrownBy(() -> service.completeTask(WorkflowHumanTaskCompleteCommand.builder()
                .taskId("task-1")
                .nodeType("TITLE_CONFIRM")
                .userId(2002L)
                .version(task.getVersion())
                .result(Map.of("selectedMainTitle", "主标题"))
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权完成人工任务");
    }

    @Test
    void shouldExpireWaitingHumanTaskWhenDeadlinePassed() {
        WorkflowHumanTaskService service = new WorkflowHumanTaskService(new InMemoryHumanTaskStore());
        WorkflowHumanTask task = service.createWaitingTask(WorkflowHumanTaskCreateCommand.builder()
                .taskId("task-1")
                .bizType(CreationTypeEnum.ARTICLE.getValue())
                .nodeType("TITLE_CONFIRM")
                .assigneeUserId(1001L)
                .inputSnapshotJson("{}")
                .formSchemaJson("{}")
                .expireTime(LocalDateTime.now().minusMinutes(1))
                .build());

        assertThatThrownBy(() -> service.completeTask(WorkflowHumanTaskCompleteCommand.builder()
                .taskId("task-1")
                .nodeType("TITLE_CONFIRM")
                .userId(1001L)
                .version(task.getVersion())
                .result(Map.of("selectedMainTitle", "主标题"))
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("人工任务已过期");

        assertThat(task.getStatus()).isEqualTo(HumanTaskStatusEnum.EXPIRED.getValue());
    }
}
