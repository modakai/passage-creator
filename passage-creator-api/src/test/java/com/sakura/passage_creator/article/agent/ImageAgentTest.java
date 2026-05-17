package com.sakura.passage_creator.article.agent;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.image.service.ArticleImageStorageService;
import com.sakura.passage_creator.article.image.service.ImageGenerationResult;
import com.sakura.passage_creator.article.image.service.ImageTool;
import com.sakura.passage_creator.article.image.service.ImageToolRegistry;
import com.sakura.passage_creator.article.model.dto.image.ImageData;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageAgentTest {

    @Test
    void shouldCallToolSelectedByRequirementAndUploadResult() {
        ArticleImageStorageService storageService = mock(ArticleImageStorageService.class);
        ImageTool pexelsTool = fixedTool(ImageMethodEnum.PEXELS, "pexels");
        ImageAgent imageAgent = new ImageAgent(null, new ImageToolRegistry(List.of(pexelsTool)), storageService);
        when(storageService.uploadArticleImage(eq("task-1"), any(ImageData.class)))
                .thenReturn("https://oss.example.com/pexels.png");

        List<ArticleState.ImageResult> results = imageAgent.generateImages(
                "task-1",
                List.of(requirement(ImageMethodEnum.PEXELS)),
                null
        );

        assertThat(results).singleElement()
                .satisfies(result -> {
                    assertThat(result.getMethod()).isEqualTo(ImageMethodEnum.PEXELS.getValue());
                    assertThat(result.getUrl()).isEqualTo("https://oss.example.com/pexels.png");
                });
    }

    @Test
    void shouldFallbackToPicsumWhenSelectedToolFails() {
        ArticleImageStorageService storageService = mock(ArticleImageStorageService.class);
        ImageTool failingPexelsTool = failingTool(ImageMethodEnum.PEXELS);
        ImageTool picsumTool = fixedTool(ImageMethodEnum.PICSUM, "fallback");
        ImageAgent imageAgent = new ImageAgent(null, new ImageToolRegistry(List.of(failingPexelsTool, picsumTool)), storageService);
        when(storageService.uploadArticleImage(eq("task-1"), any(ImageData.class)))
                .thenReturn("https://oss.example.com/fallback.png");

        List<ArticleState.ImageResult> results = imageAgent.generateImages(
                "task-1",
                List.of(requirement(ImageMethodEnum.PEXELS)),
                null
        );

        assertThat(results).singleElement()
                .satisfies(result -> {
                    assertThat(result.getMethod()).isEqualTo(ImageMethodEnum.PICSUM.getValue());
                    assertThat(result.getUrl()).isEqualTo("https://oss.example.com/fallback.png");
                });
    }

    private ArticleState.ImageRequirement requirement(ImageMethodEnum method) {
        ArticleState.ImageRequirement requirement = new ArticleState.ImageRequirement();
        requirement.setPosition(2);
        requirement.setType("section");
        requirement.setImageSource(method.getValue());
        requirement.setPlaceholderId("IMAGE_PLACEHOLDER_1");
        requirement.setKeywords("productivity");
        requirement.setPrompt("A productivity illustration");
        requirement.setSectionTitle("效率提升");
        return requirement;
    }

    private ImageTool fixedTool(ImageMethodEnum method, String payload) {
        return new ImageTool() {
            @Override
            public ImageMethodEnum getMethod() {
                return method;
            }

            @Override
            public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
                return ImageGenerationResult.builder()
                        .method(method)
                        .imageData(ImageData.builder()
                                .bytes(payload.getBytes(StandardCharsets.UTF_8))
                                .mimeType("image/png")
                                .extension(".png")
                                .build())
                        .build();
            }
        };
    }

    private ImageTool failingTool(ImageMethodEnum method) {
        return new ImageTool() {
            @Override
            public ImageMethodEnum getMethod() {
                return method;
            }

            @Override
            public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
                throw new IllegalStateException("tool failed");
            }
        };
    }
}
