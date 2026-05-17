package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
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

class ArticleImageGenerationServiceTest {

    @Test
    void shouldUseStrategyMatchingRequirementImageSource() {
        ArticleImageStorageService storageService = mock(ArticleImageStorageService.class);
        ImageGenerateStrategy pexelsStrategy = new FixedImageGenerateStrategy(ImageMethodEnum.PEXELS, "pexels");
        ImageGenerateStrategy gptImageStrategy = new FixedImageGenerateStrategy(ImageMethodEnum.GPT_IMAGE, "gpt");
        ArticleImageGenerationService service = new ArticleImageGenerationService(
                List.of(pexelsStrategy, gptImageStrategy),
                storageService
        );
        when(storageService.uploadArticleImage(eq("task-1"), any(ImageData.class)))
                .thenReturn("https://oss.example.com/pexels.png");

        List<ArticleState.ImageResult> results = service.generateImages(
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
    void shouldUsePicsumFallbackWhenRequestedStrategyFails() {
        ArticleImageStorageService storageService = mock(ArticleImageStorageService.class);
        ImageGenerateStrategy failingStrategy = new FailingImageGenerateStrategy(ImageMethodEnum.PEXELS);
        ImageGenerateStrategy fallbackStrategy = new FixedImageGenerateStrategy(ImageMethodEnum.PICSUM, "fallback");
        ArticleImageGenerationService service = new ArticleImageGenerationService(
                List.of(failingStrategy, fallbackStrategy),
                storageService
        );
        when(storageService.uploadArticleImage(eq("task-1"), any(ImageData.class)))
                .thenReturn("https://oss.example.com/fallback.png");

        List<ArticleState.ImageResult> results = service.generateImages(
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
        requirement.setPrompt("A useful editorial image");
        requirement.setKeywords("productivity");
        requirement.setSectionTitle("效率提升");
        return requirement;
    }

    private ImageData imageData(String text) {
        return ImageData.builder()
                .bytes(text.getBytes(StandardCharsets.UTF_8))
                .mimeType("image/png")
                .extension(".png")
                .build();
    }

    private class FixedImageGenerateStrategy implements ImageGenerateStrategy {

        private final ImageMethodEnum method;

        private final String payload;

        private FixedImageGenerateStrategy(ImageMethodEnum method, String payload) {
            this.method = method;
            this.payload = payload;
        }

        @Override
        public ImageMethodEnum getMethod() {
            return method;
        }

        @Override
        public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
            return ImageGenerationResult.builder()
                    .method(method)
                    .imageData(imageData(payload))
                    .build();
        }
    }

    private class FailingImageGenerateStrategy implements ImageGenerateStrategy {

        private final ImageMethodEnum method;

        private FailingImageGenerateStrategy(ImageMethodEnum method) {
            this.method = method;
        }

        @Override
        public ImageMethodEnum getMethod() {
            return method;
        }

        @Override
        public ImageGenerationResult generate(ArticleState.ImageRequirement requirement) {
            throw new IllegalStateException("strategy failed");
        }
    }
}
