package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.config.PexelsImageProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PexelsImageGenerateStrategyTest {

    @Test
    void shouldExtractLarge2xPhotoUrlFromPexelsResponse() {
        PexelsImageGenerateStrategy strategy = new PexelsImageGenerateStrategy(
                new PexelsImageProperties(),
                null
        );
        String responseBody = """
                {
                  "photos": [
                    {
                      "src": {
                        "large2x": "https://images.pexels.com/photo-large2x.jpg",
                        "large": "https://images.pexels.com/photo-large.jpg"
                      }
                    }
                  ]
                }
                """;

        String imageUrl = strategy.extractFirstPhotoUrl(responseBody);

        assertThat(imageUrl).isEqualTo("https://images.pexels.com/photo-large2x.jpg");
    }
}
