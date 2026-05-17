package com.sakura.passage_creator.creation.workflow;

import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.entity.Article;
import com.sakura.passage_creator.article.model.enums.ArticlePhaseEnum;
import com.sakura.passage_creator.article.model.enums.ArticleStatusEnum;
import com.sakura.passage_creator.article.service.impl.ArticleServiceImpl;
import com.sakura.passage_creator.article.workflow.ArticleWorkflowAdapter;
import io.github.linpeilie.Converter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * 文章 workflow 适配器测试，确保业务结果仍写入 article 业务模型。
 */
class ArticleWorkflowAdapterTest {

    @Test
    void shouldBuildArticleStateFromWorkflowContextAndPersistTitleOptions() {
        ArticleServiceImpl articleService = spy(new ArticleServiceImpl(mock(Converter.class)));
        Article article = new Article();
        article.setTaskId("task-1");
        article.setUserId(1001L);
        article.setTopic("AI 写作");
        article.setEnabledImageMethods("[\"GPT_IMAGE\"]");
        doReturn(article).when(articleService).getOne(any(QueryWrapper.class));
        doReturn(true).when(articleService)
                .saveTitleOptions(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq("task-1"));
        doReturn(true).when(articleService).updateStatus(ArticleStatusEnum.PROCESSING, "task-1");
        doReturn(true).when(articleService).updateStatus(ArticleStatusEnum.PENDING, "task-1");
        doReturn(true).when(articleService).updatePhase(ArticlePhaseEnum.TITLE_GENERATING, "task-1");
        doReturn(true).when(articleService).updatePhase(ArticlePhaseEnum.TITLE_SELECTING, "task-1");
        ArticleWorkflowAdapter adapter = new ArticleWorkflowAdapter(articleService);

        ArticleState state = adapter.toArticleState(WorkflowContext.fromMap(java.util.Map.of("taskId", "task-1")));
        ArticleState.TitleOption titleOption = new ArticleState.TitleOption();
        titleOption.setMainTitle("主标题");
        titleOption.setSubTitle("副标题");
        adapter.saveTitleOptions("task-1", List.of(titleOption));

        assertThat(state.getTaskId()).isEqualTo("task-1");
        assertThat(state.getUserId()).isEqualTo(1001L);
        assertThat(state.getTopic()).isEqualTo("AI 写作");
        assertThat(state.getEnabledImageMethods()).containsExactly("GPT_IMAGE");
    }

    @Test
    void shouldSerializeConfirmedOutlineIntoWorkflowContextShape() {
        ArticleState.OutlineSection section = new ArticleState.OutlineSection();
        section.setSection(1);
        section.setTitle("第一节");
        section.setPoints(List.of("观点"));
        ArticleState.OutlineResult outline = new ArticleState.OutlineResult();
        outline.setSections(List.of(section));

        WorkflowContext context = WorkflowContext.fromMap(java.util.Map.of(
                "confirmedOutline", JSONUtil.parseObj(outline)
        ));

        ArticleState.OutlineResult restored = context.getBean("confirmedOutline", ArticleState.OutlineResult.class);

        assertThat(restored.getSections()).singleElement()
                .satisfies(restoredSection -> assertThat(restoredSection.getTitle()).isEqualTo("第一节"));
    }
}
