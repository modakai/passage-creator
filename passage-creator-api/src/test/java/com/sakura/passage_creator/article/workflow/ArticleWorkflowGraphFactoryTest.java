package com.sakura.passage_creator.article.workflow;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.sakura.passage_creator.creation.workflow.checkpoint.RedisWorkflowCheckpointSaver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 文章 workflow Graph 测试，确保文章流程真正由 Spring AI Alibaba StateGraph 编排。
 */
class ArticleWorkflowGraphFactoryTest {

    @Test
    void shouldCreateGraphFactoryBeanWithSpringConstructorInjection() {
        // 工厂存在测试用构造器时，必须显式验证 Spring 仍然选择生产依赖注入构造器。
        new ApplicationContextRunner()
                .withBean(ArticleTitleNodeHandler.class, () -> mock(ArticleTitleNodeHandler.class))
                .withBean(ArticleTitleConfirmedNodeAction.class, () -> mock(ArticleTitleConfirmedNodeAction.class))
                .withBean(ArticleOutlineNodeHandler.class, () -> mock(ArticleOutlineNodeHandler.class))
                .withBean(ArticleOutlineConfirmedNodeAction.class, () -> mock(ArticleOutlineConfirmedNodeAction.class))
                .withBean(ArticleContentNodeHandler.class, () -> mock(ArticleContentNodeHandler.class))
                .withBean(ArticleImageAnalyzeNodeHandler.class, () -> mock(ArticleImageAnalyzeNodeHandler.class))
                .withBean(ArticleImageGenerateNodeHandler.class, () -> mock(ArticleImageGenerateNodeHandler.class))
                .withBean(ArticleContentMergeNodeHandler.class, () -> mock(ArticleContentMergeNodeHandler.class))
                .withBean(RedisWorkflowCheckpointSaver.class, () -> mock(RedisWorkflowCheckpointSaver.class))
                .withBean(ArticleWorkflowGraphFactory.class)
                .run(context -> assertThat(context).hasSingleBean(ArticleWorkflowGraphFactory.class));
    }

    @Test
    void shouldInterruptAfterTitleAndResumeFromSpringAiAlibabaCheckpoint() throws Exception {
        List<String> executedNodes = new ArrayList<>();
        NodeAction titleAction = state -> {
            executedNodes.add(ArticleWorkflowNodeType.TITLE_GENERATING.getValue());
            return Map.of("titleOptions", List.of("标题 A"));
        };
        NodeAction outlineAction = state -> {
            executedNodes.add(ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue());
            return Map.of("outline", "大纲 A");
        };
        ArticleWorkflowGraphFactory factory = new ArticleWorkflowGraphFactory(
                titleAction,
                state -> Map.of("selectedMainTitle", "标题 A"),
                outlineAction,
                state -> Map.of("confirmedOutline", "大纲 A"),
                state -> Map.of("content", "正文"),
                state -> Map.of("imageRequirements", List.of()),
                state -> Map.of("images", List.of()),
                state -> Map.of("fullContent", "完整正文")
        );

        RunnableConfig firstConfig = RunnableConfig.builder().threadId("article-task-1").build();
        NodeOutput firstOutput = factory.compile().invokeAndGetOutput(
                Map.of("taskId", "article-task-1"),
                firstConfig
        ).orElseThrow();

        assertThat(firstOutput.node()).isEqualTo(ArticleWorkflowNodeType.TITLE_GENERATING.getValue());
        assertThat(firstOutput.state().value("titleOptions")).isPresent();
        assertThat(executedNodes).containsExactly(ArticleWorkflowNodeType.TITLE_GENERATING.getValue());

        RunnableConfig resumeConfig = RunnableConfig.builder()
                .threadId("article-task-1")
                .resume()
                .build();
        NodeOutput outlineOutput = factory.compile().invokeAndGetOutput(Map.of(), resumeConfig).orElseThrow();

        assertThat(outlineOutput.node()).isEqualTo(ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue());
        assertThat(outlineOutput.state().value("outline")).contains("大纲 A");
        assertThat(executedNodes).containsExactly(
                ArticleWorkflowNodeType.TITLE_GENERATING.getValue(),
                ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue()
        );

        NodeOutput finalOutput = factory.compile().invokeAndGetOutput(Map.of(), resumeConfig).orElseThrow();
        assertThat(finalOutput.isEND()).isTrue();
        assertThat(finalOutput.state().value("fullContent")).contains("完整正文");
        assertThat(executedNodes).containsExactly(
                ArticleWorkflowNodeType.TITLE_GENERATING.getValue(),
                ArticleWorkflowNodeType.OUTLINE_GENERATING.getValue()
        );
    }
}
