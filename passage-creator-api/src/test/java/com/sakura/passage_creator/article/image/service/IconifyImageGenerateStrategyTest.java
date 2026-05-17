package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.config.IconifyImageProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IconifyImageGenerateStrategyTest {

    @Test
    void shouldExtractFirstIconAndBuildSvgUrl() {
        IconifyImageProperties properties = new IconifyImageProperties();
        properties.setDefaultHeight(80);
        IconifyImageGenerateStrategy strategy = new IconifyImageGenerateStrategy(properties, null);

        String iconName = strategy.extractFirstIconName("""
                {"icons": ["mdi:calendar", "mdi:clock"]}
                """);
        String svgUrl = strategy.buildSvgUrl(iconName);

        assertThat(iconName).isEqualTo("mdi:calendar");
        assertThat(svgUrl).isEqualTo("https://api.iconify.design/mdi/calendar.svg?height=80");
    }
}
