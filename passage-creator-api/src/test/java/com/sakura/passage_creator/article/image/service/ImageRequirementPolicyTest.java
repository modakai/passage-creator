package com.sakura.passage_creator.article.image.service;

import com.sakura.passage_creator.article.agent.state.ArticleState;
import com.sakura.passage_creator.article.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageRequirementPolicyTest {

    @Test
    void shouldKeepCoverAndAtMostTwoSectionImages() {
        ImageRequirementPolicy policy = new ImageRequirementPolicy(2);

        List<ArticleState.ImageRequirement> limited = policy.apply(List.of(
                requirement(1, "cover", ""),
                requirement(2, "section", "{{IMAGE_PLACEHOLDER_1}}"),
                requirement(3, "section", "{{IMAGE_PLACEHOLDER_2}}"),
                requirement(4, "section", "{{IMAGE_PLACEHOLDER_3}}")
        ));

        assertThat(limited).extracting(ArticleState.ImageRequirement::getPosition)
                .containsExactly(1, 2, 3);
    }

    @Test
    void shouldKeepSupportedImageSourceInsteadOfForcingGptImage() {
        ImageRequirementPolicy policy = new ImageRequirementPolicy(2);

        List<ArticleState.ImageRequirement> limited = policy.apply(List.of(
                requirement(1, "cover", "", ImageMethodEnum.PEXELS.getValue()),
                requirement(2, "section", "{{IMAGE_PLACEHOLDER_1}}", ImageMethodEnum.ICONIFY.getValue())
        ));

        assertThat(limited).extracting(ArticleState.ImageRequirement::getImageSource)
                .containsExactly(ImageMethodEnum.PEXELS.getValue(), ImageMethodEnum.ICONIFY.getValue());
    }

    @Test
    void shouldFallbackUnsupportedImageSourceToGptImage() {
        ImageRequirementPolicy policy = new ImageRequirementPolicy(2);

        List<ArticleState.ImageRequirement> limited = policy.apply(List.of(
                requirement(1, "cover", "", "NANO_BANANA"),
                requirement(2, "section", "{{IMAGE_PLACEHOLDER_1}}", null)
        ));

        assertThat(limited).extracting(ArticleState.ImageRequirement::getImageSource)
                .containsExactly(ImageMethodEnum.GPT_IMAGE.getValue(), ImageMethodEnum.GPT_IMAGE.getValue());
    }

    @Test
    void shouldRespectAllowedImageMethodsFromUserSelection() {
        ImageRequirementPolicy policy = new ImageRequirementPolicy(2, List.of(ImageMethodEnum.PEXELS.getValue()));

        List<ArticleState.ImageRequirement> limited = policy.apply(List.of(
                requirement(1, "cover", "", ImageMethodEnum.GPT_IMAGE.getValue()),
                requirement(2, "section", "{{IMAGE_PLACEHOLDER_1}}", ImageMethodEnum.ICONIFY.getValue())
        ));

        assertThat(limited).extracting(ArticleState.ImageRequirement::getImageSource)
                .containsExactly(ImageMethodEnum.PEXELS.getValue(), ImageMethodEnum.PEXELS.getValue());
    }

    private ArticleState.ImageRequirement requirement(Integer position, String type, String placeholderId) {
        return requirement(position, type, placeholderId, ImageMethodEnum.GPT_IMAGE.getValue());
    }

    private ArticleState.ImageRequirement requirement(Integer position, String type, String placeholderId, String imageSource) {
        ArticleState.ImageRequirement requirement = new ArticleState.ImageRequirement();
        requirement.setPosition(position);
        requirement.setType(type);
        requirement.setImageSource(imageSource);
        requirement.setPrompt("A clean editorial illustration");
        requirement.setPlaceholderId(placeholderId);
        return requirement;
    }
}
